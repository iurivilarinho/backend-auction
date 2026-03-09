package com.br.leilao.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.leilao.enums.AuctionStatus;
import com.br.leilao.enums.LotType;
import com.br.leilao.models.Auction;
import com.br.leilao.models.AuctionItem;
import com.br.leilao.response.AuctionItemResponse;
import com.br.leilao.response.AuctionJsonResponse;
import com.br.leilao.response.AuctionListJsonResponse;
import com.br.leilao.response.AuctionListResponse;
import com.br.leilao.response.AuctionResponse;
import com.br.leilao.service.AuctionDetranService;
import com.br.leilao.service.AuctionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Leilões", description = "Endpoints relacionados a leilões do sistema e integração com site do Detran")
public class AuctionController {

	private final AuctionService auctionService;
	private final AuctionDetranService detranService;

	public AuctionController(AuctionService auctionService, AuctionDetranService detranService) {
		this.auctionService = auctionService;
		this.detranService = detranService;
	}

	@Operation(summary = "Listar leilões do sistema", description = "Retorna uma lista paginada de leilões armazenados no banco de dados.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/auctions")
	public ResponseEntity<Page<AuctionListResponse>> findAll(@RequestParam(required = false) List<AuctionStatus> status,
			@RequestParam(required = false) String search, Pageable pageable) {

		Page<Auction> auctions = auctionService.findAll(status, search, pageable);
		return ResponseEntity.ok(auctions.map(AuctionListResponse::new));
	}

	@Operation(summary = "Buscar leilão por ID", description = "Retorna um leilão específico armazenado no banco de dados.")
	@ApiResponse(responseCode = "200", description = "Leilão encontrado")
	@ApiResponse(responseCode = "404", description = "Leilão não encontrado")
	@GetMapping("/auctions/{auctionId}")
	public ResponseEntity<AuctionResponse> findById(@PathVariable Long auctionId) {

		Auction auction = auctionService.findById(auctionId);
		return ResponseEntity.ok(new AuctionResponse(auction));
	}

	@Operation(summary = "Listar itens de leilão", description = "Retorna itens de leilão armazenados no banco de dados.")
	@ApiResponse(responseCode = "200", description = "Itens retornados com sucesso")
	@GetMapping("/auction-items")
	public ResponseEntity<Page<AuctionItemResponse>> findAllItems(
			@RequestParam(required = false) List<AuctionStatus> auctionStatus,
			@RequestParam(required = false) List<LotType> type, @RequestParam(required = false) String search,
			Pageable pageable) {

		Page<AuctionItem> items = auctionService.findAllItems(auctionStatus, type, search, pageable);
		return ResponseEntity.ok(items.map(AuctionItemResponse::new));
	}

	@Operation(summary = "Detalhes de lotes do leilão (Detran)", description = "Retorna dados convertidos diretamente do site do Detran. Não são dados persistidos em banco.")
	@ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
	@GetMapping("/json/site/details/auction")
	public AuctionJsonResponse getAuctionLots() throws IOException {
		return detranService.fetchAuctionLots();
	}

	@Operation(summary = "Lista de leilões do site do Detran", description = "Retorna leilões obtidos diretamente do site do Detran, sem persistência em banco.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	@GetMapping("/json/site/auctions")
	public List<AuctionListJsonResponse> getAuctions() throws IOException {
		return detranService.fetchAuctions();
	}
}