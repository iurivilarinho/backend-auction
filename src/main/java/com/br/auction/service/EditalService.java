package com.br.auction.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.models.Auction;

/**
 * Baixa o edital (PDF publico) de um leilao do DETRAN-MG e guarda no proprio {@link Auction},
 * para ficar disponivel na nossa base mesmo se o site sair do ar. URL publica (sem login):
 * {@code /documentos-leiloes/edital/{detranAuctionId}/{ano}}.
 */
@Service
public class EditalService {

	private static final Logger LOG = LoggerFactory.getLogger(EditalService.class);

	private final String panelUrl;
	private final String editalPath;
	private final String userAgent;
	private final int timeoutMs;

	public EditalService(
			@Value("${detran.panel.url:https://leilao.detran.mg.gov.br}") String panelUrl,
			@Value("${detran.edital.path:/documentos-leiloes/edital}") String editalPath,
			@Value("${auction.source.user-agent}") String userAgent,
			@Value("${auction.source.timeout-ms:30000}") int timeoutMs) {
		this.panelUrl = panelUrl.replaceAll("/+$", "");
		this.editalPath = editalPath;
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
	}

	/**
	 * Garante que o edital do leilao esteja guardado: se ainda nao houver e o leilao tiver
	 * id/ano do DETRAN, baixa e popula os campos do {@link Auction} (sem salvar). Best-effort:
	 * retorna {@code true} se passou a ter edital.
	 */
	public boolean populate(Auction auction) {
		if (auction == null || auction.hasEdital()) {
			return false;
		}
		String id = auction.getDetranAuctionId();
		String year = auction.getAuctionYear();
		if (id == null || id.isBlank() || year == null || year.isBlank()) {
			return false;
		}
		String url = panelUrl + editalPath + "/" + id.trim() + "/" + year.trim();
		// O endpoint de edital do DETRAN responde 500 de forma intermitente; tentamos algumas vezes.
		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				HttpResponse<byte[]> resp = newClient().send(
						HttpRequest.newBuilder(URI.create(url))
								.header("User-Agent", userAgent)
								.header("Accept", "application/pdf,text/html")
								.timeout(Duration.ofMillis(timeoutMs))
								.GET().build(),
						HttpResponse.BodyHandlers.ofByteArray());
				String contentType = resp.headers().firstValue("Content-Type").orElse("");
				if (resp.statusCode() < 300 && isPdf(resp.body(), contentType)) {
					auction.setEditalBytes(resp.body());
					auction.setEditalContentType("application/pdf");
					auction.setEditalFileName("edital-leilao-" + id.trim() + "-" + year.trim() + ".pdf");
					return true;
				}
				LOG.debug("Edital indisponivel leilao {}/{} (tentativa {}/3, HTTP {})", id, year, attempt,
						resp.statusCode());
			} catch (Exception ex) {
				LOG.debug("Falha ao baixar edital {}/{} (tentativa {}/3): {}", id, year, attempt, ex.getMessage());
			}
			sleepQuietly(700L * attempt);
		}
		return false;
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean isPdf(byte[] body, String contentType) {
		if (contentType != null && contentType.toLowerCase().contains("pdf")) {
			return body != null && body.length > 0;
		}
		return body != null && body.length > 4 && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F';
	}

	private HttpClient newClient() throws Exception {
		return HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.ALWAYS)
				.connectTimeout(Duration.ofSeconds(15))
				.sslContext(trustAll())
				.build();
	}

	private SSLContext trustAll() throws Exception {
		TrustManager[] tm = { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] c, String a) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] c, String a) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} };
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, tm, new SecureRandom());
		return ctx;
	}
}
