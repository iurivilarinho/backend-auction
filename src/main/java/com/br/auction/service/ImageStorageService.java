package com.br.auction.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.br.auction.models.AuctionItem;
import com.br.auction.models.AuctionItemImage;

/**
 * Baixa e materializa as imagens dos veiculos no banco da aplicacao. Como os bytes ficam
 * persistidos, as imagens continuam disponiveis mesmo que o provedor de origem saia do ar.
 *
 * <p>Suporta URLs http(s) e data URIs (data:image/...;base64,...), o que permite receber
 * imagens diretamente no payload de uma integracao inbound.</p>
 */
@Service
public class ImageStorageService {

	private static final Logger LOG = LoggerFactory.getLogger(ImageStorageService.class);
	private static final String DATA_URI_PREFIX = "data:";

	private final RestClient restClient;

	public ImageStorageService(RestClient integrationRestClient) {
		this.restClient = integrationRestClient;
	}

	/**
	 * Substitui todas as imagens do item. Para data URIs (data:image/...) baixa e armazena os
	 * bytes no banco; para URLs http(s) (fotos reais do provedor) guarda apenas a URL, evitando
	 * baixar milhares de imagens no scraping. A foto real continua sendo exibida pela URL.
	 */
	public void replaceImages(AuctionItem item, List<String> urls) {
		item.clearImages();
		if (urls == null) {
			return;
		}
		for (String url : urls) {
			if (url == null || url.isBlank()) {
				continue;
			}
			String trimmed = url.trim();
			if (trimmed.startsWith(DATA_URI_PREFIX)) {
				AuctionItemImage image = download(trimmed);
				if (image != null) {
					item.addImage(image);
				}
			} else {
				item.addImage(new AuctionItemImage(trimmed, null, null));
			}
		}
	}

	/**
	 * Baixa uma imagem e devolve a entidade (sem vincular ao item). Retorna null em falha,
	 * para que uma imagem indisponivel nao interrompa a integracao do registro.
	 */
	public AuctionItemImage download(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		String trimmed = url.trim();
		try {
			if (trimmed.startsWith(DATA_URI_PREFIX)) {
				return fromDataUri(trimmed);
			}
			return fromHttp(trimmed);
		} catch (RuntimeException ex) {
			LOG.warn("Nao foi possivel baixar a imagem {}: {}", trimmed, ex.getMessage());
			return null;
		}
	}

	private AuctionItemImage fromHttp(String url) {
		ResponseEntity<byte[]> response = restClient.get()
				.uri(url)
				.retrieve()
				.toEntity(byte[].class);
		byte[] data = response.getBody();
		if (data == null || data.length == 0) {
			return null;
		}
		MediaType contentType = response.getHeaders().getContentType();
		String type = contentType != null ? contentType.toString() : "image/jpeg";
		return new AuctionItemImage(url, type, data);
	}

	private AuctionItemImage fromDataUri(String dataUri) {
		int comma = dataUri.indexOf(',');
		if (comma < 0) {
			return null;
		}
		String meta = dataUri.substring(DATA_URI_PREFIX.length(), comma);
		String payload = dataUri.substring(comma + 1);
		boolean base64 = meta.contains(";base64");
		String contentType = meta.split(";")[0];
		if (contentType.isBlank()) {
			contentType = "image/png";
		}
		byte[] data = base64
				? Base64.getDecoder().decode(payload)
				: payload.getBytes(StandardCharsets.UTF_8);
		return new AuctionItemImage(null, contentType, data);
	}

	public List<AuctionItemImage> buildImages(List<String> urls) {
		List<AuctionItemImage> images = new ArrayList<>();
		if (urls == null) {
			return images;
		}
		for (String url : urls) {
			AuctionItemImage image = download(url);
			if (image != null) {
				images.add(image);
			}
		}
		return images;
	}
}
