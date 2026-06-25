package com.br.auction.garage.acquisition;

import java.io.ByteArrayInputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.garage.models.Acquisition;
import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.credential.CredentialRepository;

/**
 * Integra com o painel do provedor (DETRAN-MG / perfil ARREMATANTE) para baixar automaticamente
 * a pagina de arremates. O login passa pelo SSO SAML do Seg.ID (PRODEMGE): a autenticacao em si e
 * um FORM JEE padrao ({@code j_security_check} com {@code j_username}/{@code j_password}); apos o
 * SAML ha a selecao do perfil de trabalho ({@code papel_trabalho=SDLL0030} = ARREMATANTE). Usamos
 * {@link HttpClient} (cookies por dominio + redirects automaticos) e o Jsoup apenas para ler os
 * formularios auto-submit (SAMLRequest/SAMLResponse) e os tokens CSRF do CakePHP.
 */
@Service
public class DetranPanelService {

	private static final Logger LOG = LoggerFactory.getLogger(DetranPanelService.class);
	private static final String PANEL_CREDENTIAL_CODE = "DETRAN_MG_PANEL";

	public record SyncResult(boolean success, String message, int documentsAdded) {
	}

	public record FetchResult(boolean success, String message, String html) {
	}

	private final CredentialRepository credentialRepository;
	private final String panelUrl;
	private final String arrematesPath;
	private final String papel;
	private final String configCpf;
	private final String configPassword;
	private final String userAgent;
	private final int timeoutMs;

	public DetranPanelService(CredentialRepository credentialRepository,
			@Value("${detran.panel.url:https://leilao.detran.mg.gov.br}") String panelUrl,
			@Value("${detran.panel.arremates-path:/arremates}") String arrematesPath,
			@Value("${detran.panel.papel:SDLL0030}") String papel,
			@Value("${detran.panel.cpf:}") String configCpf,
			@Value("${detran.panel.password:}") String configPassword,
			@Value("${auction.source.user-agent}") String userAgent,
			@Value("${auction.source.timeout-ms:30000}") int timeoutMs) {
		this.credentialRepository = credentialRepository;
		this.panelUrl = panelUrl.replaceAll("/+$", "");
		this.arrematesPath = arrematesPath;
		this.papel = papel;
		this.configCpf = configCpf;
		this.configPassword = configPassword;
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
	}

	/**
	 * Autentica no painel (SAML + selecao do perfil ARREMATANTE) e devolve o HTML da pagina de
	 * arremates. Best-effort: em caso de falha de login/credencial, retorna sucesso=false e o
	 * usuario pode colar o HTML manualmente.
	 */
	public FetchResult fetchArrematesHtml() {
		String[] creds = resolveCredentials();
		if (creds == null) {
			return new FetchResult(false,
					"Credencial do painel nao configurada. Defina detran.panel.cpf/password ou cadastre a credencial '"
							+ PANEL_CREDENTIAL_CODE + "' em Integracoes > Credenciais.",
					null);
		}
		String cpf = creds[0];
		String password = creds[1];

		try {
			HttpClient client = newClient();

			// 1) GET /arremates -> redireciona para a tela de login com o form SAMLRequest.
			HttpResponse<byte[]> r = get(client, panelUrl + arrematesPath);
			Document doc = parse(r);
			Element samlForm = firstFormWith(doc, "SAMLRequest");
			if (samlForm == null) {
				if (hasArrematesTable(doc)) {
					return new FetchResult(true, "Arremates obtidos do painel.", doc.outerHtml());
				}
				return new FetchResult(false,
						"Nao foi possivel iniciar o login no painel (form SAML nao encontrado).", null);
			}

			// 2) POST SAMLRequest -> IdP Seg.ID (PRODEMGE), pagina de login.
			r = postForm(client, resolveAction(samlForm, r), formData(samlForm, Map.of()));
			String idpBase = baseContext(r.uri().toString());

			// 3) POST j_security_check com CPF/senha (FORM JEE padrao).
			r = postForm(client, idpBase + "j_security_check",
					Map.of("j_username", cpf, "j_password", password));
			Document current = parse(r);

			// 4) Reenvia os formularios auto-submit (SAMLResponse de volta ao ACS) ate sair do SSO.
			for (int hop = 0; hop < 6; hop++) {
				Element auto = firstFormWith(current, "SAMLResponse");
				if (auto == null) {
					auto = firstFormWith(current, "SAMLRequest");
				}
				if (auto == null) {
					break;
				}
				r = postForm(client, resolveAction(auto, r), formData(auto, Map.of()));
				current = parse(r);
			}

			if (isLoginPage(current)) {
				return new FetchResult(false,
						"Falha ao autenticar no painel (CPF/senha invalidos ou SSO indisponivel).", null);
			}

			// 5) Seleciona o perfil de trabalho ARREMATANTE (selecionar-papel + tokens CSRF).
			selectPapel(client);

			// 6) GET /arremates autenticado.
			r = get(client, panelUrl + arrematesPath);
			Document arremates = parse(r);
			if (isLoginPage(arremates)) {
				return new FetchResult(false,
						"Login realizado, mas a sessao do painel nao foi mantida ao abrir /arremates.", null);
			}
			return new FetchResult(true, "Arremates obtidos do painel (perfil arrematante).", arremates.outerHtml());
		} catch (Exception ex) {
			LOG.warn("Falha ao baixar arremates do painel DETRAN: {}", ex.getMessage(), ex);
			return new FetchResult(false, "Nao foi possivel acessar o painel do provedor: " + ex.getMessage(), null);
		}
	}

	/** Seleciona o perfil ARREMATANTE quando a tela de selecao de perfil estiver presente. */
	private void selectPapel(HttpClient client) {
		try {
			HttpResponse<byte[]> sel = get(client,
					panelUrl + "/ssc/login/selecionar-unidade?redirect=" + enc(arrematesPath));
			Document doc = parse(sel);
			Element form = doc.selectFirst("form[action*=selecionar-papel]");
			if (form == null) {
				return;
			}
			Map<String, String> overrides = new LinkedHashMap<>();
			overrides.put("papel_trabalho", papel);
			overrides.put("redirect", arrematesPath);
			postForm(client, resolveAction(form, sel), formData(form, overrides));
		} catch (Exception ex) {
			LOG.debug("Selecao de perfil nao aplicada: {}", ex.getMessage());
		}
	}

	// ------------------------------------------------------------- HTTP helpers

	private HttpClient newClient() throws Exception {
		CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		return HttpClient.newBuilder()
				.cookieHandler(cookies)
				// ALWAYS (e nao NORMAL) porque o painel redireciona de HTTPS para HTTP no fluxo SSO.
				.followRedirects(HttpClient.Redirect.ALWAYS)
				.connectTimeout(Duration.ofSeconds(15))
				.sslContext(trustAllSslContext())
				.build();
	}

	private HttpResponse<byte[]> get(HttpClient client, String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", userAgent)
				.header("Accept", "text/html,application/xhtml+xml")
				.timeout(Duration.ofMillis(timeoutMs))
				.GET()
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	private HttpResponse<byte[]> postForm(HttpClient client, String url, Map<String, String> data) throws Exception {
		String body = urlEncode(data);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", userAgent)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "text/html,application/xhtml+xml")
				.timeout(Duration.ofMillis(timeoutMs))
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	private Document parse(HttpResponse<byte[]> response) throws Exception {
		return Jsoup.parse(new ByteArrayInputStream(response.body()), null, response.uri().toString());
	}

	/** Form que contem um input com o nome informado (ex.: SAMLRequest/SAMLResponse). */
	private Element firstFormWith(Document doc, String inputName) {
		return doc.selectFirst("form:has(input[name=" + inputName + "])");
	}

	/** Coleta os inputs/selects do form + sobrescritas; valores nulos viram string vazia. */
	private Map<String, String> formData(Element form, Map<String, String> overrides) {
		Map<String, String> data = new LinkedHashMap<>();
		for (Element input : form.select("input[name]")) {
			data.put(input.attr("name"), input.attr("value"));
		}
		for (Element select : form.select("select[name]")) {
			Element selected = select.selectFirst("option[selected]");
			data.put(select.attr("name"), selected != null ? selected.attr("value") : "");
		}
		data.putAll(overrides);
		return data;
	}

	private String resolveAction(Element form, HttpResponse<byte[]> response) {
		String action = form.absUrl("action");
		if (action == null || action.isBlank()) {
			action = response.uri().toString();
		}
		return action;
	}

	/** Contexto base do IdP (ex.: https://host/ssc-idp-frontend/) a partir de uma URL dele. */
	private String baseContext(String url) {
		URI uri = URI.create(url);
		String path = uri.getPath() == null ? "/" : uri.getPath();
		int secondSlash = path.indexOf('/', 1);
		String context = secondSlash > 0 ? path.substring(0, secondSlash + 1) : "/";
		return uri.getScheme() + "://" + uri.getAuthority() + context;
	}

	private boolean isLoginPage(Document doc) {
		return doc.selectFirst("input[name=j_password]") != null
				|| doc.selectFirst("input[name=SAMLRequest]") != null;
	}

	private boolean hasArrematesTable(Document doc) {
		return doc.selectFirst("table") != null && doc.text().toLowerCase().contains("arremat");
	}

	private String urlEncode(Map<String, String> data) {
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, String> entry : data.entrySet()) {
			parts.add(enc(entry.getKey()) + "=" + enc(entry.getValue() == null ? "" : entry.getValue()));
		}
		return String.join("&", parts);
	}

	private String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private SSLContext trustAllSslContext() throws Exception {
		TrustManager[] trustAll = { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} };
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustAll, new SecureRandom());
		return context;
	}

	/** CPF/senha do arrematante: prioriza a config (seed) e cai para a credencial cadastrada. */
	private String[] resolveCredentials() {
		if (configCpf != null && !configCpf.isBlank() && configPassword != null && !configPassword.isBlank()) {
			return new String[] { configCpf.trim(), configPassword.trim() };
		}
		Credential credential = credentialRepository.findByCode(PANEL_CREDENTIAL_CODE).orElse(null);
		if (credential != null && credential.getUsername() != null && !credential.getUsername().isBlank()
				&& credential.getPassword() != null && !credential.getPassword().isBlank()) {
			return new String[] { credential.getUsername().trim(), credential.getPassword().trim() };
		}
		return null;
	}

	public SyncResult syncDocuments(Acquisition acquisition) {
		// Os documentos do painel sao baixados via acoes JavaScript (links href="#"), que nao expoem
		// URLs diretas no HTML. Mantemos o anexo manual ate mapear o endpoint de download.
		LOG.info("syncDocuments solicitado para aquisicao {} (anexo manual por ora)", acquisition.getId());
		return new SyncResult(false,
				"Os documentos do painel sao gerados por acao no site e nao podem ser baixados automaticamente ainda. "
						+ "Anexe-os manualmente.",
				0);
	}
}
