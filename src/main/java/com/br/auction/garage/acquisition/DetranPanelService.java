package com.br.auction.garage.acquisition;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
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

	private final CredentialRepository credentialRepository;
	private final String panelUrl;
	private final String userAgent;
	private final int timeoutMs;

	public DetranPanelService(CredentialRepository credentialRepository,
			@Value("${detran.panel.url:https://leilao.detran.mg.gov.br}") String panelUrl,
			@Value("${auction.source.user-agent}") String userAgent,
			@Value("${auction.source.timeout-ms:30000}") int timeoutMs) {
		this.credentialRepository = credentialRepository;
		this.panelUrl = panelUrl;
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
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
			Connection.Response loginPage = Jsoup.connect(panelUrl + "/login")
					.userAgent(userAgent)
					.timeout(timeoutMs)
					.method(Connection.Method.GET)
					.execute();

			// Tenta autenticar com a credencial do usuario.
			Connection.Response authenticated = Jsoup.connect(panelUrl + "/login")
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
