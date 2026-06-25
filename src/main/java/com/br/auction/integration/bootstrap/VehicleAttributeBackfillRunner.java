package com.br.auction.integration.bootstrap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.response.VehicleInfo;
import com.br.auction.service.VehicleParserService;

/**
 * Preenche marca/modelo/ano dos itens ja existentes que foram coletados antes da extracao desses
 * atributos. Idempotente: so processa itens com marca ainda nula e descricao preenchida; quando nao
 * houver nenhum, nao faz nada. Os itens novos ja chegam com os atributos preenchidos pela coleta.
 */
@Component
@Order(30)
public class VehicleAttributeBackfillRunner implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(VehicleAttributeBackfillRunner.class);

	private final AuctionItemRepository auctionItemRepository;
	private final VehicleParserService vehicleParserService;

	public VehicleAttributeBackfillRunner(AuctionItemRepository auctionItemRepository,
			VehicleParserService vehicleParserService) {
		this.auctionItemRepository = auctionItemRepository;
		this.vehicleParserService = vehicleParserService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		List<AuctionItem> pending = auctionItemRepository.findByBrandIsNullAndVehicleDescriptionIsNotNull();
		if (pending.isEmpty()) {
			return;
		}
		for (AuctionItem item : pending) {
			VehicleInfo info = vehicleParserService.parse(item.getVehicleDescription());
			item.setBrand(info.getBrand());
			item.setModel(info.getModel());
			item.setVehicleYear(info.getYear());
		}
		auctionItemRepository.saveAll(pending);
		LOG.info("Backfill de marca/modelo/ano aplicado em {} itens existentes.", pending.size());
	}
}
