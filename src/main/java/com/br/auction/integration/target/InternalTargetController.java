package com.br.auction.integration.target;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Expoe os modelos internos da aplicacao que servem de destino fixo das integracoes.
 * Esses modelos ja existem no dominio (Auction, AuctionItem) e nao precisam ser
 * cadastrados; o frontend usa este catalogo para montar o editor de de->para.
 */
@RestController
@RequestMapping("/api/integration/target-models")
@Tag(name = "Integracao - Modelos de destino", description = "Catalogo dos modelos internos da aplicacao usados como destino fixo")
public class InternalTargetController {

	@Operation(summary = "Listar modelos internos de destino", description = "Retorna os modelos internos e seus campos disponiveis para o de->para.")
	@ApiResponse(responseCode = "200", description = "Catalogo retornado com sucesso")
	@GetMapping
	public ResponseEntity<List<InternalTargetModelResponse>> findAll() {
		List<InternalTargetModelResponse> models = Arrays.stream(InternalTargetModel.values())
				.map(InternalTargetModelResponse::new)
				.toList();
		return ResponseEntity.ok(models);
	}
}
