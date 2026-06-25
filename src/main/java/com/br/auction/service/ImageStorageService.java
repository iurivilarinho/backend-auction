package com.br.auction.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private static final int MAX_IMAGES_PER_LOT = 30;
	// Padrao das fotos do provedor: .../img_{lotId}_{n}.jpg — a lista entrega apenas o _1.
	private static final java.util.regex.Pattern SEQUENTIAL_IMAGE = java.util.regex.Pattern
			.compile("^(.*_)(\\d+)(\\.[A-Za-z0-9]+)$");

	private final RestClient restClient;

	public ImageStorageService(RestClient integrationRestClient) {
		this.restClient = integrationRestClient;
	}

	/**
	 * Substitui as imagens do item, baixando e armazenando os bytes no banco para que as fotos
	 * continuem disponiveis mesmo que o provedor saia do ar. Os bytes ja persistidos para a mesma
	 * URL sao reaproveitados, de modo que as execucoes recorrentes (a cada 15 min) nao rebaixem
	 * imagens inalteradas. Se o download falhar, guarda ao menos a URL de origem como referencia.
	 */
	public void replaceImages(AuctionItem item, List<String> urls) {
		Map<String, AuctionItemImage> alreadyStored = new HashMap<>();
		for (AuctionItemImage existing : item.getImages()) {
			if (existing.getSourceUrl() != null && existing.getData() != null && !existing.getData().isBlank()) {
				alreadyStored.putIfAbsent(existing.getSourceUrl(), existing);
			}
		}
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
				continue;
			}
			AuctionItemImage cached = alreadyStored.get(trimmed);
			if (cached != null) {
				item.addImage(reuse(cached));
				continue;
			}
			AuctionItemImage downloaded = download(trimmed);
			item.addImage(downloaded != null ? downloaded : new AuctionItemImage(trimmed, null, null));
		}
	}

	/**
	 * Descobre e baixa TODAS as fotos de um lote. A pagina de lista entrega apenas a primeira foto
	 * ({@code img_{lotId}_1.jpg}); as demais seguem o mesmo padrao sequencial. A descoberta roda uma
	 * unica vez por lote (controlada por {@code imagesSynced}) para nao re-baixar a cada coleta de
	 * 15 min. Itens ja coletados antes desta funcionalidade (imagesSynced nulo) sao reprocessados
	 * na proxima coleta e passam a ter a galeria completa.
	 */
	public void syncLotImages(AuctionItem item, List<String> urls) {
		if (Boolean.TRUE.equals(item.getImagesSynced())) {
			return;
		}
		item.clearImages();
		String seed = null;
		if (urls != null) {
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
				} else if (seed == null) {
					seed = trimmed;
				}
			}
		}

		if (seed != null) {
			java.util.regex.Matcher matcher = SEQUENTIAL_IMAGE.matcher(seed);
			if (matcher.matches()) {
				String prefix = matcher.group(1);
				int start = Integer.parseInt(matcher.group(2));
				String extension = matcher.group(3);
				// O provedor responde 200 com uma imagem placeholder fixa para indices inexistentes
				// (em vez de 404). Descobrimos a assinatura desse placeholder sondando um indice bem
				// alto e paramos a sequencia quando a foto baixada for igual a ele (ou repetida).
				AuctionItemImage placeholder = download(prefix + (start + 200) + extension);
				String placeholderData = placeholder == null ? null : placeholder.getData();
				java.util.Set<String> seenData = new java.util.HashSet<>();
				for (int index = start; index < start + MAX_IMAGES_PER_LOT; index++) {
					AuctionItemImage image = download(prefix + index + extension);
					if (image == null) {
						break;
					}
					String data = image.getData();
					if (placeholderData != null && placeholderData.equals(data)) {
						break; // chegou no placeholder de "sem imagem"
					}
					if (data != null && !seenData.add(data)) {
						break; // imagem repetida indica fim da galeria real
					}
					item.addImage(image);
				}
			} else {
				AuctionItemImage image = download(seed);
				item.addImage(image != null ? image : new AuctionItemImage(seed, null, null));
			}
		}

		// So marca como sincronizado quando ao menos uma foto foi obtida; do contrario tenta de novo
		// na proxima coleta (evita travar com 0 fotos quando o provedor estava indisponivel).
		if (!item.getImages().isEmpty()) {
			item.setImagesSynced(Boolean.TRUE);
		}
	}

	/**
	 * Cria uma nova imagem reaproveitando os bytes ja persistidos (evita rebaixar do provedor).
	 */
	private AuctionItemImage reuse(AuctionItemImage source) {
		AuctionItemImage copy = new AuctionItemImage();
		copy.setSourceUrl(source.getSourceUrl());
		copy.setContentType(source.getContentType());
		copy.setData(source.getData());
		return copy;
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
