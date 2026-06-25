package com.br.auction.garage.acquisition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Connection;
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
 * Integra com o painel do provedor (DETRAN-MG) usando a credencial cadastrada para, quando
 * possivel, coletar os documentos do veiculo adquirido (carta de arrematacao, edital, alvara)
 * e persisti-los no banco. A coleta e tolerante a falhas: se o painel estiver indisponivel ou
 * o layout nao puder ser interpretado, retorna uma mensagem e os documentos podem ser
 * cadastrados manualmente.
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
	private final String loginPath;
	private final String arrematesPath;
	private final String userAgent;
	private final int timeoutMs;

	public DetranPanelService(CredentialRepository credentialRepository,
			@Value("${detran.panel.url:https://leilao.detran.mg.gov.br}") String panelUrl,
			@Value("${detran.panel.login-path:/ssc/login/login}") String loginPath,
			@Value("${detran.panel.arremates-path:/arremates}") String arrematesPath,
			@Value("${auction.source.user-agent}") String userAgent,
			@Value("${auction.source.timeout-ms:30000}") int timeoutMs) {
		this.credentialRepository = credentialRepository;
		this.panelUrl = panelUrl;
		this.loginPath = loginPath;
		this.arrematesPath = arrematesPath;
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
	}

	/**
	 * Faz login no painel com a credencial salva (perfil do arrematante) e baixa o HTML da pagina
	 * {@code /arremates}. Carrega os campos ocultos do formulario de login (tokens CSRF do CakePHP)
	 * para que a autenticacao funcione. E best-effort: se o login falhar ou o layout mudar, retorna
	 * sucesso=false e o usuario ainda pode colar o HTML manualmente.
	 */
	public FetchResult fetchArrematesHtml() {
		Credential credential = credentialRepository.findByCode(PANEL_CREDENTIAL_CODE).orElse(null);
		if (credential == null) {
			return new FetchResult(false,
					"Credencial do painel (" + PANEL_CREDENTIAL_CODE
							+ ") nao cadastrada. Cadastre-a em Integracoes > Credenciais.",
					null);
		}
		String username = credential.getUsername();
		String password = credential.getPassword();
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			return new FetchResult(false, "Credencial do painel sem usuario/senha configurados.", null);
		}

		try {
			Connection.Response loginPage = Jsoup.connect(panelUrl + loginPath)
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.method(Connection.Method.GET)
					.execute();

			Document loginDoc = loginPage.parse();
			// O painel DETRAN-MG usa SSO federado (SAML/PRODEMGE/gov.br): o formulario de login
			// redireciona para um provedor de identidade externo. Esse fluxo nao pode ser concluido
			// apenas com CPF/senha no servidor, entao detectamos e instruimos o uso do HTML manual.
			if (isFederatedSsoLogin(loginDoc)) {
				return new FetchResult(false,
						"O painel DETRAN-MG usa login federado (gov.br/SSO), que nao pode ser automatizado apenas com CPF e senha. "
								+ "Faca login no painel pelo navegador e use 'Colar HTML' com a pagina de arremates.",
						null);
			}

			Map<String, String> form = new LinkedHashMap<>();
			for (Element input : loginDoc.select("form input")) {
				String name = input.attr("name");
				if (!name.isBlank()) {
					form.put(name, input.attr("value"));
				}
			}
			// Preenche os possiveis nomes de campo usados pelo painel para login do arrematante.
			form.put("cpf", username);
			form.put("usuario", username);
			form.put("login", username);
			form.put("senha", password);
			form.put("password", password);

			Connection.Response authenticated = Jsoup.connect(panelUrl + loginPath)
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.cookies(loginPage.cookies())
					.data(form)
					.method(Connection.Method.POST)
					.followRedirects(true)
					.execute();

			Map<String, String> sessionCookies = new LinkedHashMap<>(loginPage.cookies());
			sessionCookies.putAll(authenticated.cookies());

			Connection.Response arremates = Jsoup.connect(panelUrl + arrematesPath)
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.cookies(sessionCookies)
					.method(Connection.Method.GET)
					.followRedirects(true)
					.execute();

			Document arrematesDoc = arremates.parse();
			if (looksLikeLoginPage(arrematesDoc)) {
				return new FetchResult(false,
						"Nao foi possivel autenticar no painel automaticamente (login federado/SSO ou credencial invalida). "
								+ "Use 'Colar HTML' com a pagina de arremates aberta apos login no navegador.",
						null);
			}
			return new FetchResult(true, "Arremates obtidos do painel do provedor.", arrematesDoc.html());
		} catch (Exception ex) {
			LOG.warn("Falha ao baixar arremates do painel DETRAN: {}", ex.getMessage());
			return new FetchResult(false, "Nao foi possivel acessar o painel do provedor: " + ex.getMessage(), null);
		}
	}

	private boolean isFederatedSsoLogin(Document document) {
		// Formulario com SAMLRequest ou action para provedor de identidade externo (ex.: PRODEMGE).
		return document.selectFirst("input[name=SAMLRequest]") != null
				|| !document.select("form[action*=idp], form[action*=prodemge], form[action*=govbr]").isEmpty();
	}

	private boolean looksLikeLoginPage(Document document) {
		// Se a pagina ainda tem campo de senha ou SAMLRequest, a sessao nao foi autenticada.
		return document.selectFirst("input[type=password]") != null
				|| document.selectFirst("input[name=SAMLRequest]") != null
				|| !document.select("form[action*=login]").isEmpty();
	}

	public SyncResult syncDocuments(Acquisition acquisition) {
		Credential credential = credentialRepository.findByCode(PANEL_CREDENTIAL_CODE).orElse(null);
		if (credential == null) {
			return new SyncResult(false,
					"Credencial do painel (" + PANEL_CREDENTIAL_CODE + ") nao cadastrada. Cadastre-a em Integracoes > Credenciais.",
					0);
		}
		String username = credential.getUsername();
		String password = credential.getPassword();
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			return new SyncResult(false, "Credencial do painel sem usuario/senha configurados.", 0);
		}

		try {
			// Abre a pagina de login para capturar cookies/token.
			Connection.Response loginPage = Jsoup.connect(panelUrl + loginPath)
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.method(Connection.Method.GET)
					.execute();

			// Tenta autenticar com a credencial do usuario.
			Connection.Response authenticated = Jsoup.connect(panelUrl + loginPath)
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.cookies(loginPage.cookies())
					.data("cpf", username)
					.data("usuario", username)
					.data("senha", password)
					.method(Connection.Method.POST)
					.execute();

			LOG.info("Tentativa de acesso ao painel DETRAN para o veiculo {} retornou HTTP {}", acquisition.getId(),
					authenticated.statusCode());

			// O layout autenticado do painel nao esta disponivel neste ambiente para extracao
			// automatica dos documentos. Informa o usuario para cadastro manual quando preciso.
			return new SyncResult(false,
					"Acesso ao painel realizado, mas os documentos nao puderam ser extraidos automaticamente neste ambiente. "
							+ "Voce pode anexa-los manualmente.",
					0);
		} catch (Exception ex) {
			LOG.warn("Falha ao acessar o painel DETRAN: {}", ex.getMessage());
			return new SyncResult(false, "Nao foi possivel acessar o painel do provedor: " + ex.getMessage(), 0);
		}
	}
}
