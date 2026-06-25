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
import java.text.Normalizer;
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
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.garage.enums.DocumentType;
import com.br.auction.integration.credential.Credential;
import com.br.auction.integration.credential.CredentialRepository;

/**
 * Integra com o painel do provedor (DETRAN-MG / perfil ARREMATANTE): autentica via SSO SAML do
 * Seg.ID (PRODEMGE), abre a pagina de arremates e baixa os documentos (Nota, Carta e Alvara) de
 * cada veiculo arrematado. A autenticacao em si e um FORM JEE padrao
 * ({@code j_security_check}); apos o SAML ha a selecao do perfil de trabalho
 * ({@code papel_trabalho=SDLL0030} = ARREMATANTE). Cada documento e um POST para
 * {@code /arremates/{nota|carta|alvara}-...} com o id do arremate + tokens CSRF do CakePHP, que
 * devolve o PDF — guardado no banco para ficar disponivel mesmo se o site sair do ar.
 */
@Service
public class DetranPanelService {

	private static final Logger LOG = LoggerFactory.getLogger(DetranPanelService.class);
	private static final String PANEL_CREDENTIAL_CODE = "DETRAN_MG_PANEL";

	public record SyncResult(boolean success, String message, int documentsAdded) {
	}

	/** Documento (PDF) baixado do painel. */
	public record PanelDoc(DocumentType type, String fileName, String contentType, byte[] bytes) {
	}

	/** Um veiculo arrematado com seus documentos baixados do painel. */
	public record PanelArremate(String leilao, String lote, String descricao, String valor, String condicao,
			String status, List<PanelDoc> documents) {

		public String reference() {
			return "Leilao " + (leilao == null ? "?" : leilao) + " / Lote " + (lote == null ? "?" : lote);
		}
	}

	/** Resultado da coleta no painel. */
	public record PanelResult(boolean success, String message, List<PanelArremate> arremates) {
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
	 * Autentica no painel e coleta os arremates do perfil arrematante. Quando {@code withDocuments}
	 * e verdadeiro, baixa tambem os PDFs (Nota, Carta e Alvara) de cada veiculo.
	 */
	public PanelResult fetchArremates(boolean withDocuments) {
		String[] creds = resolveCredentials();
		if (creds == null) {
			return new PanelResult(false,
					"Credencial do painel nao configurada. Defina detran.panel.cpf/password ou cadastre a credencial '"
							+ PANEL_CREDENTIAL_CODE + "' em Integracoes > Credenciais.",
					List.of());
		}
		try {
			HttpClient client = newClient();
			Document arremates = authenticateAndOpenArremates(client, creds[0], creds[1]);
			List<PanelArremate> list = parseArremates(client, arremates, withDocuments);
			if (list.isEmpty()) {
				return new PanelResult(true, "Nenhum veiculo arrematado encontrado no painel.", list);
			}
			int docs = list.stream().mapToInt(a -> a.documents().size()).sum();
			return new PanelResult(true,
					list.size() + " arremate(s) no painel" + (withDocuments ? " (" + docs + " documento(s))." : "."),
					list);
		} catch (PanelException ex) {
			return new PanelResult(false, ex.getMessage(), List.of());
		} catch (Exception ex) {
			LOG.warn("Falha ao coletar arremates do painel DETRAN: {}", ex.getMessage(), ex);
			return new PanelResult(false, "Nao foi possivel acessar o painel do provedor: " + ex.getMessage(),
					List.of());
		}
	}

	/** Compatibilidade: HTML cru da pagina autenticada de arremates (sem documentos). */
	public FetchResult fetchArrematesHtml() {
		String[] creds = resolveCredentials();
		if (creds == null) {
			return new FetchResult(false, "Credencial do painel nao configurada.", null);
		}
		try {
			HttpClient client = newClient();
			Document arremates = authenticateAndOpenArremates(client, creds[0], creds[1]);
			return new FetchResult(true, "Arremates obtidos do painel.", arremates.outerHtml());
		} catch (PanelException ex) {
			return new FetchResult(false, ex.getMessage(), null);
		} catch (Exception ex) {
			return new FetchResult(false, "Nao foi possivel acessar o painel: " + ex.getMessage(), null);
		}
	}

	public record FetchResult(boolean success, String message, String html) {
	}

	// ------------------------------------------------------------- autenticacao SSO

	private Document authenticateAndOpenArremates(HttpClient client, String cpf, String password) throws Exception {
		// 1) GET /arremates -> tela de login com o form SAMLRequest.
		HttpResponse<byte[]> r = get(client, panelUrl + arrematesPath);
		Document doc = parse(r);
		Element samlForm = firstFormWith(doc, "SAMLRequest");
		if (samlForm == null) {
			if (hasArrematesTable(doc)) {
				return doc; // ja autenticado
			}
			throw new PanelException("Nao foi possivel iniciar o login no painel (form SAML nao encontrado).");
		}

		// 2) POST SAMLRequest -> IdP Seg.ID (PRODEMGE).
		r = postForm(client, resolveAction(samlForm, r), formData(samlForm, Map.of()));
		String idpBase = baseContext(r.uri().toString());

		// 3) POST j_security_check com CPF/senha (FORM JEE padrao).
		r = postForm(client, idpBase + "j_security_check", Map.of("j_username", cpf, "j_password", password));
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
			throw new PanelException("Falha ao autenticar no painel (CPF/senha invalidos ou SSO indisponivel).");
		}

		// 5) Seleciona o perfil ARREMATANTE (selecionar-papel + tokens CSRF).
		selectPapel(client);

		// 6) GET /arremates autenticado.
		Document arremates = parse(get(client, panelUrl + arrematesPath));
		if (isLoginPage(arremates)) {
			throw new PanelException("Login realizado, mas a sessao do painel nao foi mantida ao abrir /arremates.");
		}
		return arremates;
	}

	private void selectPapel(HttpClient client) {
		try {
			HttpResponse<byte[]> sel = get(client,
					panelUrl + "/ssc/login/selecionar-unidade?redirect=" + enc(arrematesPath));
			Element form = parse(sel).selectFirst("form[action*=selecionar-papel]");
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

	// ------------------------------------------------------------- parsing + documentos

	private List<PanelArremate> parseArremates(HttpClient client, Document doc, boolean withDocuments) {
		List<PanelArremate> out = new ArrayList<>();
		Element table = findArrematesTable(doc);
		if (table == null) {
			return out;
		}
		List<String> headers = new ArrayList<>();
		Element headerRow = table.selectFirst("tr");
		if (headerRow != null) {
			for (Element th : headerRow.select("th, td")) {
				headers.add(norm(th.text()));
			}
		}
		int iLeilao = indexOfHeader(headers, "leilao");
		int iLote = indexOfHeader(headers, "lote");
		int iDesc = indexOfHeader(headers, "descricao");
		int iValor = indexOfHeader(headers, "valor");
		int iCond = indexOfHeader(headers, "condicao");
		int iStatus = indexOfHeader(headers, "status");

		Elements rows = table.select("tbody tr");
		if (rows.isEmpty()) {
			rows = table.select("tr");
		}
		for (Element row : rows) {
			Elements cells = row.select("td");
			if (cells.isEmpty()) {
				continue;
			}
			String descricao = cell(cells, iDesc);
			if (descricao == null) {
				continue;
			}
			List<PanelDoc> docs = withDocuments ? downloadRowDocuments(client, row) : List.of();
			out.add(new PanelArremate(cell(cells, iLeilao), cell(cells, iLote), descricao, cell(cells, iValor),
					cell(cells, iCond), cell(cells, iStatus), docs));
		}
		return out;
	}

	/** Baixa os PDFs (Nota/Carta/Alvara) dos forms ocultos da coluna "Acoes" de uma linha. */
	private List<PanelDoc> downloadRowDocuments(HttpClient client, Element row) {
		List<PanelDoc> docs = new ArrayList<>();
		for (Element form : row.select("form[action*=arremat]")) {
			DocumentType type = documentType(form.attr("action"));
			if (type == null) {
				continue;
			}
			PanelDoc doc = downloadDocument(client, form, type);
			if (doc != null) {
				docs.add(doc);
			}
		}
		return docs;
	}

	/**
	 * Baixa um documento (PDF). O painel do DETRAN as vezes responde 500 de forma intermitente na
	 * geracao da Nota/Carta, entao tentamos algumas vezes antes de desistir (o que faltar e
	 * completado na proxima importacao).
	 */
	private PanelDoc downloadDocument(HttpClient client, Element form, DocumentType type) {
		String action = resolveAction(form, panelUrl + arrematesPath);
		Map<String, String> data = formData(form, Map.of());
		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				HttpResponse<byte[]> resp = postForm(client, action, data);
				String contentType = resp.headers().firstValue("Content-Type").orElse("");
				if (resp.statusCode() < 300 && looksLikePdf(resp.body(), contentType)) {
					return new PanelDoc(type, fileName(resp, type), "application/pdf", resp.body());
				}
				LOG.debug("Documento {} indisponivel (tentativa {}/3, HTTP {}, ct '{}')", type, attempt,
						resp.statusCode(), contentType);
			} catch (Exception ex) {
				LOG.debug("Falha ao baixar documento {} (tentativa {}/3): {}", type, attempt, ex.getMessage());
			}
			sleepQuietly(800L * attempt);
		}
		return null;
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private DocumentType documentType(String action) {
		String a = action == null ? "" : action.toLowerCase();
		if (a.contains("carta")) {
			return DocumentType.CARTA_ARREMATACAO;
		}
		if (a.contains("nota")) {
			return DocumentType.NOTA_ARREMATACAO;
		}
		if (a.contains("alvara")) {
			return DocumentType.ALVARA_LIBERACAO;
		}
		return null;
	}

	private boolean looksLikePdf(byte[] body, String contentType) {
		if (contentType != null && contentType.toLowerCase().contains("pdf")) {
			return body != null && body.length > 0;
		}
		return body != null && body.length > 4 && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F';
	}

	private String fileName(HttpResponse<byte[]> resp, DocumentType type) {
		String cd = resp.headers().firstValue("Content-Disposition").orElse("");
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("filename=\"?([^\";]+)\"?").matcher(cd);
		String name = m.find() ? m.group(1).trim() : type.name().toLowerCase();
		if (!name.toLowerCase().endsWith(".pdf")) {
			name = name + ".pdf";
		}
		return name;
	}

	private Element findArrematesTable(Document doc) {
		for (Element table : doc.select("table")) {
			String header = table.select("th").text().toLowerCase();
			if (header.contains("lote") && header.contains("valor")) {
				return table;
			}
		}
		return null;
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
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", userAgent)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "text/html,application/xhtml+xml,application/pdf")
				.timeout(Duration.ofMillis(timeoutMs))
				.POST(HttpRequest.BodyPublishers.ofString(urlEncode(data), StandardCharsets.UTF_8))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	private Document parse(HttpResponse<byte[]> response) throws Exception {
		return Jsoup.parse(new ByteArrayInputStream(response.body()), null, response.uri().toString());
	}

	private Element firstFormWith(Document doc, String inputName) {
		return doc.selectFirst("form:has(input[name=" + inputName + "])");
	}

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
		return resolveAction(form, response.uri().toString());
	}

	private String resolveAction(Element form, String fallback) {
		String action = form.absUrl("action");
		return action == null || action.isBlank() ? fallback : action;
	}

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
		return findArrematesTable(doc) != null;
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

	private int indexOfHeader(List<String> headers, String keyword) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i).contains(keyword)) {
				return i;
			}
		}
		return -1;
	}

	private String cell(Elements cells, int index) {
		if (index < 0 || index >= cells.size()) {
			return null;
		}
		String text = cells.get(index).text().trim();
		return text.isEmpty() ? null : text;
	}

	private String norm(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase().trim();
		return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
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

	/** Falha de negocio do painel com mensagem amigavel (login/SSO/sessao). */
	private static class PanelException extends RuntimeException {
		PanelException(String message) {
			super(message);
		}
	}
}
