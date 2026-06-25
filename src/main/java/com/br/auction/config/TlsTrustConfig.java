package com.br.auction.config;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Quando a validacao de certificados TLS da fonte esta desligada
 * ({@code auction.source.validate-tls-certificates=false}), instala um contexto TLS
 * permissivo como padrao da JVM. Isso garante que todos os clientes HTTP (RestClient das
 * integracoes, download de imagens e o Jsoup do scraper) consigam acessar o provedor mesmo
 * quando a cadeia de certificados nao e reconhecida (erro PKIX path building failed).
 */
@Configuration
public class TlsTrustConfig {

	private static final Logger LOG = LoggerFactory.getLogger(TlsTrustConfig.class);

	private final boolean validateTls;

	public TlsTrustConfig(
			@Value("${auction.source.validate-tls-certificates:false}") boolean validateTls) {
		this.validateTls = validateTls;
	}

	@PostConstruct
	public void configurePermissiveTlsIfNeeded() {
		if (validateTls) {
			return;
		}
		try {
			TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
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
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, new SecureRandom());
			SSLContext.setDefault(sslContext);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
			LOG.info("TLS permissivo configurado para acesso a fonte externa (validacao de certificados desligada).");
		} catch (Exception ex) {
			throw new IllegalStateException("Nao foi possivel configurar o TLS permissivo.", ex);
		}
	}
}
