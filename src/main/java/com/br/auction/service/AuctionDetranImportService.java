package com.br.auction.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.AuctionSyncResultResponse;
import com.br.auction.response.LotResponse;

@Service
public class AuctionDetranImportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetranImportService.class);
	private static final DateTimeFormatter BRAZIL_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final AuctionRepository auctionRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final AuctionDetranService detranService;
	private final FipeService fipeService;
	private final ImageStorageService imageStorageService;
	private final boolean schedulerEnabled;
	private final boolean fipeImportEnabled;

	public AuctionDetranImportService(AuctionRepository auctionRepository, AuctionItemRepository auctionItemRepository,
			AuctionDetranService detranService, FipeService fipeService, ImageStorageService imageStorageService,
			@Value("${auction.scheduler.enabled:false}") boolean schedulerEnabled,
			@Value("${auction.import.fipe.enabled:false}") boolean fipeImportEnabled) {
		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.detranService = detranService;
		this.fipeService = fipeService;
		this.imageStorageService = imageStorageService;
		this.schedulerEnabled = schedulerEnabled;
		this.fipeImportEnabled = fipeImportEnabled;
	}

	/**
	 * Sincronizacao recorrente com o provedor (a cada 15 minutos). Como ninguem dispara dados
	 * de la pra ca, a coleta e feita ativamente aqui, de forma incremental (upsert): atualiza
	 * os valores de lance e a situacao dos lotes a cada execucao.
	 */
	@Transactional
	@Scheduled(fixedDelayString = "${auction.scheduler.interval-ms:900000}", initialDelayString = "${auction.scheduler.initial-delay-ms:15000}")
	public void importAllAuctions() {
		if (!schedulerEnabled) {
			return;
		}
		try {
			AuctionSyncResultResponse result = syncAuctions(AuctionProvider.defaultProvider(), new AuctionSourceFilter());
			LOGGER.info("Sincronizacao automatica concluida: {} leiloes na fonte, {} importados, {} atualizados, {} itens.",
					result.getTotalSourceAuctions(), result.getImportedAuctions(), result.getUpdatedAuctions(),
					result.getImportedItems());
		} catch (RuntimeException ex) {
			LOGGER.warn("Falha na sincronizacao automatica com o provedor: {}", ex.getMessage());
		}
	}

	@Transactional
	public AuctionSyncResultResponse syncAuctions(AuctionProvider provider, AuctionSourceFilter filter) {
		LocalDateTime startedAt = LocalDateTime.now();
		int importedAuctions = 0;
		int updatedAuctions = 0;
		int skippedAuctions = 0;
		int importedItems = 0;
		int skippedItems = 0;

		try {
			List<AuctionListJsonResponse> sourceAuctions = detranService.fetchAuctions(provider, filter);

			for (AuctionListJsonResponse sourceAuction : sourceAuctions) {
				if (sourceAuction.getAuctionId() == null || sourceAuction.getAuctionId().isBlank()) {
					skippedAuctions++;
					continue;
				}

				Optional<Auction> existingAuction = auctionRepository.findByProviderCodeAndDetranAuctionId(
						provider.getCode(), sourceAuction.getAuctionId());
				Auction auction = existingAuction.orElseGet(Auction::new);
				boolean isNewAuction = existingAuction.isEmpty();

				applyAuctionSource(auction, sourceAuction, provider);
				Auction savedAuction = auctionRepository.save(auction);

				if (isNewAuction) {
					importedAuctions++;
				} else {
					updatedAuctions++;
				}

				AuctionJsonResponse lots = detranService.fetchAuctionLots(provider, sourceAuction.getAuctionId(),
						sourceAuction.getAuctionYear());
				ImportItemCounter counter = importAuctionLots(savedAuction, lots.getLots());
				importedItems += counter.getImportedItems();
				skippedItems += counter.getSkippedItems();
			}

			return new AuctionSyncResultResponse(provider, sourceAuctions.size(), importedAuctions, updatedAuctions,
					skippedAuctions, importedItems, skippedItems, startedAt, LocalDateTime.now());
		} catch (IOException ex) {
			throw new IllegalStateException("Nao foi possivel sincronizar leiloes do provedor " + provider.getCode(), ex);
		}
	}

	private ImportItemCounter importAuctionLots(Auction auction, List<LotResponse> lots) {
		if (lots == null || lots.isEmpty()) {
			return new ImportItemCounter(0, 0);
		}

		int importedItems = 0;
		int skippedItems = 0;

		for (LotResponse lot : lots) {
			if (lot.getLotId() == null || lot.getLotId().isBlank()) {
				skippedItems++;
				continue;
			}

			Optional<AuctionItem> existingItem = auctionItemRepository.findByAuctionIdAndLotId(auction.getId(),
					lot.getLotId());
			AuctionItem item = existingItem.orElseGet(AuctionItem::new);

			item.setAuction(auction);
			item.setLotId(lot.getLotId());
			item.setLotNumber(lot.getLotNumber());
			item.setLotType(lot.getLotType());
			item.setVehicleDescription(lot.getVehicleDescription());
			item.setCurrentBidValue(parseMoney(lot.getCurrentBidValue()));
			imageStorageService.replaceImages(item, lot.getImageUrls());

			if (fipeImportEnabled && item.getVehicleDescription() != null && !item.getVehicleDescription().isBlank()) {
				item.setFipeValue(fipeService.getFipeValue(item.getVehicleDescription()));
			}

			auctionItemRepository.save(item);
			if (existingItem.isEmpty()) {
				importedItems++;
			} else {
				skippedItems++;
			}
		}

		return new ImportItemCounter(importedItems, skippedItems);
	}

	private void applyAuctionSource(Auction auction, AuctionListJsonResponse sourceAuction, AuctionProvider provider) {
		auction.setAuctionNoticeNumber(sourceAuction.getAuctionNoticeNumber());
		auction.setCity(sourceAuction.getCity());
		auction.setAuctioneer(sourceAuction.getAuctioneer());
		auction.setStatus(sourceAuction.getStatus());
		auction.setClosingDate(parseClosingDate(sourceAuction.getClosingDate()));
		auction.setDetranAuctionId(sourceAuction.getAuctionId());
		auction.setAuctionYear(sourceAuction.getAuctionYear());
		auction.setSourceUrl(sourceAuction.getSourceUrl());
		auction.setProviderCode(provider.getCode());
		auction.setProviderName(provider.getName());
		auction.setStateCode(provider.getStateCode());
		auction.setStateName(provider.getStateName());
	}

	private LocalDateTime parseClosingDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return LocalDateTime.parse(value.trim(), BRAZIL_DATE_TIME);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private BigDecimal parseMoney(String value) {
		if (value == null || value.isBlank()) {
			return BigDecimal.ZERO;
		}

		String normalized = value.replace("R$", "").replace(".", "").replace(",", ".").trim();
		if (normalized.isBlank()) {
			return BigDecimal.ZERO;
		}

		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private static class ImportItemCounter {
		private final int importedItems;
		private final int skippedItems;

		ImportItemCounter(int importedItems, int skippedItems) {
			this.importedItems = importedItems;
			this.skippedItems = skippedItems;
		}

		int getImportedItems() {
			return importedItems;
		}

		int getSkippedItems() {
			return skippedItems;
		}
	}
}
