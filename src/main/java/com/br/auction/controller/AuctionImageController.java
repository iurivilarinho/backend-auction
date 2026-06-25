package com.br.auction.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.models.AuctionItemImage;
import com.br.auction.repository.AuctionItemImageRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;

/**
 * Serve as imagens dos veiculos diretamente do banco da aplicacao, garantindo que
 * continuem disponiveis mesmo que o provedor de origem fique fora do ar.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Imagens", description = "Imagens de veiculos armazenadas no banco")
public class AuctionImageController {

	private static final long CACHE_DAYS = 30;

	private final AuctionItemImageRepository repository;

	public AuctionImageController(AuctionItemImageRepository repository) {
		this.repository = repository;
	}

	@Operation(summary = "Obter imagem do veiculo", description = "Retorna os bytes de uma imagem persistida no banco.")
	@ApiResponse(responseCode = "200", description = "Imagem retornada")
	@ApiResponse(responseCode = "404", description = "Imagem nao encontrada")
	@GetMapping("/{imageId}")
	public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
		AuctionItemImage image = repository.findById(imageId)
				.orElseThrow(() -> new EntityNotFoundException("Imagem nao encontrada: " + imageId));
		MediaType mediaType = resolveMediaType(image.getContentType());
		return ResponseEntity.ok()
				.contentType(mediaType)
				.cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(CACHE_DAYS)).cachePublic())
				.body(image.getBytes());
	}

	private MediaType resolveMediaType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return MediaType.IMAGE_JPEG;
		}
		try {
			return MediaType.parseMediaType(contentType);
		} catch (RuntimeException ex) {
			return MediaType.IMAGE_JPEG;
		}
	}
}
