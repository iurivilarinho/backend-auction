package com.br.auction.integration.bootstrap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.models.AuctionItemImage;
import com.br.auction.repository.AuctionRepository;

/**
 * Popula o banco com alguns leiloes e veiculos de exemplo quando ele esta vazio, incluindo
 * varias imagens por veiculo armazenadas no proprio banco. Garante que a aplicacao mostre
 * dados integrados com imagens mesmo offline e mesmo que o provedor esteja fora do ar.
 */
@Component
@Order(30)
public class DemoDataSeedRunner implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeedRunner.class);

	private final AuctionRepository auctionRepository;
	private final boolean seedEnabled;

	public DemoDataSeedRunner(AuctionRepository auctionRepository,
			@Value("${integration.seed.enabled:true}") boolean seedEnabled) {
		this.auctionRepository = auctionRepository;
		this.seedEnabled = seedEnabled;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!seedEnabled || auctionRepository.count() > 0) {
			return;
		}

		AuctionProvider provider = AuctionProvider.defaultProvider();

		Auction first = newAuction(provider, "DEMO-001", "001/2026", "Belo Horizonte", "Patio Central", "Publicado",
				LocalDateTime.now().plusDays(7));
		addItem(first, "L-1001", "1001", "CONSERVADO", "FIAT/UNO MILLE 2015", "12500.00", "28000.00", "#dbeafe");
		addItem(first, "L-1002", "1002", "CONSERVADO", "VOLKSWAGEN/GOL 1.0 2018", "19900.00", "32000.00", "#dcfce7");
		addItem(first, "L-1003", "1003", "SUCATA", "HONDA/CG 160 FAN 2020", "4200.00", "11000.00", "#fee2e2");
		auctionRepository.save(first);

		Auction second = newAuction(provider, "DEMO-002", "002/2026", "Uberlandia", "Leiloeira MG", "Em Andamento",
				LocalDateTime.now().plusDays(3));
		addItem(second, "L-2001", "2001", "CONSERVADO", "CHEVROLET/ONIX 1.4 2019", "35000.00", "55000.00", "#fef9c3");
		addItem(second, "L-2002", "2002", "CONSERVADO", "TOYOTA/COROLLA XEI 2017", "48000.00", "92000.00", "#ede9fe");
		auctionRepository.save(second);

		LOG.info("Dados de exemplo (leiloes e veiculos com imagens) cadastrados automaticamente.");
	}

	private Auction newAuction(AuctionProvider provider, String detranId, String notice, String city, String auctioneer,
			String status, LocalDateTime closingDate) {
		Auction auction = new Auction();
		auction.setDetranAuctionId(detranId);
		auction.setAuctionNoticeNumber(notice);
		auction.setCity(city);
		auction.setAuctioneer(auctioneer);
		auction.setStatus(status);
		auction.setClosingDate(closingDate);
		auction.setAuctionYear("2026");
		auction.setSourceUrl(provider.getBaseUrl());
		auction.setProviderCode(provider.getCode());
		auction.setProviderName(provider.getName());
		auction.setStateCode(provider.getStateCode());
		auction.setStateName(provider.getStateName());
		return auction;
	}

	private void addItem(Auction auction, String lotId, String lotNumber, String lotType, String description,
			String bid, String fipe, String color) {
		AuctionItem item = new AuctionItem();
		item.setAuction(auction);
		item.setLotId(lotId);
		item.setLotNumber(lotNumber);
		item.setLotType(lotType);
		item.setVehicleDescription(description);
		item.setCurrentBidValue(new BigDecimal(bid));
		item.setFipeValue(new BigDecimal(fipe));
		for (String angle : List.of("Frente", "Lateral", "Traseira")) {
			item.addImage(svgImage(description, angle, color));
		}
		auction.getItems().add(item);
	}

	private AuctionItemImage svgImage(String description, String angle, String color) {
		String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='480' height='320' viewBox='0 0 480 320'>"
				+ "<rect width='480' height='320' fill='" + color + "'/>"
				+ "<rect x='60' y='150' width='360' height='90' rx='18' fill='#0f172a' opacity='0.12'/>"
				+ "<circle cx='140' cy='240' r='26' fill='#0f172a' opacity='0.35'/>"
				+ "<circle cx='340' cy='240' r='26' fill='#0f172a' opacity='0.35'/>"
				+ "<text x='240' y='70' font-family='Arial, sans-serif' font-size='22' font-weight='700'"
				+ " fill='#0f172a' text-anchor='middle'>" + escapeXml(description) + "</text>"
				+ "<text x='240' y='110' font-family='Arial, sans-serif' font-size='16'"
				+ " fill='#0f172a' opacity='0.65' text-anchor='middle'>" + escapeXml(angle) + "</text>"
				+ "</svg>";
		return new AuctionItemImage(null, "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8));
	}

	private String escapeXml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
