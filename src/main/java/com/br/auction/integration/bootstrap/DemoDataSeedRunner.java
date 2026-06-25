package com.br.auction.integration.bootstrap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.br.auction.service.ImageStorageService;

/**
 * Popula o banco com varios leiloes e veiculos de exemplo quando ele esta vazio, simulando
 * um volume realista de editais de Minas Gerais. As fotos sao baixadas uma unica vez de uma
 * fonte publica (formando um pool) e reaproveitadas entre os veiculos, ficando armazenadas
 * no proprio banco (resilientes a indisponibilidade da fonte). Sem internet, cai para uma
 * imagem ilustrativa gerada localmente.
 */
@Component
@Order(30)
public class DemoDataSeedRunner implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeedRunner.class);
	private static final int TOTAL_AUCTIONS = 24;
	private static final int PHOTOS_PER_ITEM = 2;

	private static final String[] CITIES = { "Belo Horizonte", "Uberlandia", "Contagem", "Juiz de Fora", "Betim",
			"Montes Claros", "Ribeirao das Neves", "Uberaba", "Governador Valadares", "Ipatinga", "Sete Lagoas",
			"Divinopolis", "Pocos de Caldas", "Patos de Minas", "Pouso Alegre" };
	private static final String[] STATUSES = { "Publicado", "Em Andamento", "Publicado", "Em Andamento", "Finalizado" };
	private static final String[] AUCTIONEERS = { "Patio Central", "Leiloeira MG", "Patio Regional", "Patio Norte",
			"Leiloes Minas" };

	private record Vehicle(String description, String type, String bid, String fipe) {
	}

	private record Photo(String contentType, byte[] data) {
	}

	private static final Vehicle[] CARS = {
			new Vehicle("FIAT/UNO MILLE 2015", "car", "12500.00", "28000.00"),
			new Vehicle("VOLKSWAGEN/GOL 1.0 2018", "car", "19900.00", "32000.00"),
			new Vehicle("CHEVROLET/ONIX 1.4 2019", "car", "35000.00", "55000.00"),
			new Vehicle("TOYOTA/COROLLA XEI 2017", "car", "48000.00", "92000.00"),
			new Vehicle("HYUNDAI/HB20 1.0 2019", "car", "33000.00", "52000.00"),
			new Vehicle("FORD/KA SE 2017", "car", "27000.00", "41000.00"),
			new Vehicle("RENAULT/SANDERO 2016", "car", "24000.00", "38000.00"),
			new Vehicle("VOLKSWAGEN/SAVEIRO 2018", "car", "41000.00", "62000.00"),
			new Vehicle("FIAT/STRADA WORKING 2020", "car", "52000.00", "74000.00"),
			new Vehicle("CHEVROLET/PRISMA 2018", "car", "39000.00", "58000.00") };
	private static final Vehicle[] MOTOS = {
			new Vehicle("HONDA/CG 160 FAN 2020", "motorcycle", "9200.00", "16000.00"),
			new Vehicle("YAMAHA/FAZER 250 2019", "motorcycle", "13500.00", "21000.00"),
			new Vehicle("HONDA/BIZ 125 2018", "motorcycle", "8200.00", "13000.00") };

	private final AuctionRepository auctionRepository;
	private final ImageStorageService imageStorageService;
	private final boolean seedEnabled;
	private final boolean downloadPhotos;

	public DemoDataSeedRunner(AuctionRepository auctionRepository, ImageStorageService imageStorageService,
			@Value("${integration.seed.enabled:true}") boolean seedEnabled,
			@Value("${integration.seed.download-photos:true}") boolean downloadPhotos) {
		this.auctionRepository = auctionRepository;
		this.imageStorageService = imageStorageService;
		this.seedEnabled = seedEnabled;
		this.downloadPhotos = downloadPhotos;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!seedEnabled || auctionRepository.count() > 0) {
			return;
		}

		AuctionProvider provider = AuctionProvider.defaultProvider();
		Map<String, List<Photo>> photosByVehicle = buildPhotoMap();

		for (int i = 1; i <= TOTAL_AUCTIONS; i++) {
			Auction auction = newAuction(provider, i);
			int items = 2 + (i % 3);
			for (int j = 0; j < items; j++) {
				addItem(auction, i, j, photosByVehicle);
			}
			auctionRepository.save(auction);
		}

		LOG.info("Dados de exemplo cadastrados: {} leiloes de {} com veiculos e imagens.", TOTAL_AUCTIONS,
				provider.getName());
	}

	private Auction newAuction(AuctionProvider provider, int index) {
		Auction auction = new Auction();
		auction.setDetranAuctionId(String.format("MG-%03d", index));
		auction.setAuctionNoticeNumber(String.format("%03d/2026", index));
		auction.setCity(CITIES[(index - 1) % CITIES.length]);
		auction.setAuctioneer(AUCTIONEERS[(index - 1) % AUCTIONEERS.length]);
		auction.setStatus(STATUSES[(index - 1) % STATUSES.length]);
		auction.setClosingDate(LocalDateTime.now().plusDays((index % 30) + 1).withHour(14).withMinute(0));
		auction.setAuctionYear("2026");
		auction.setSourceUrl(provider.getBaseUrl() + "/lotes/" + index);
		auction.setProviderCode(provider.getCode());
		auction.setProviderName(provider.getName());
		auction.setStateCode(provider.getStateCode());
		auction.setStateName(provider.getStateName());
		return auction;
	}

	private void addItem(Auction auction, int auctionIndex, int itemIndex, Map<String, List<Photo>> photosByVehicle) {
		int seed = auctionIndex * 7 + itemIndex;
		boolean moto = seed % 5 == 0;
		Vehicle vehicle = moto ? MOTOS[seed % MOTOS.length] : CARS[seed % CARS.length];

		AuctionItem item = new AuctionItem();
		item.setAuction(auction);
		item.setLotId(String.format("L-%03d-%d", auctionIndex, itemIndex + 1));
		item.setLotNumber(String.valueOf(itemIndex + 1));
		item.setLotType(seed % 4 == 0 ? "SUCATA" : "CONSERVADO");
		item.setVehicleDescription(vehicle.description());
		item.setCurrentBidValue(new BigDecimal(vehicle.bid()));
		item.setFipeValue(new BigDecimal(vehicle.fipe()));

		List<Photo> photos = photosByVehicle.getOrDefault(vehicle.description(), List.of());
		if (photos.isEmpty()) {
			for (String angle : List.of("Frente", "Lateral")) {
				item.addImage(svgImage(vehicle.description(), angle));
			}
		} else {
			for (Photo photo : photos) {
				item.addImage(new AuctionItemImage(null, photo.contentType(), photo.data()));
			}
		}
		auction.getItems().add(item);
	}

	/**
	 * Baixa fotos reais especificas por modelo (ex.: "fiat,uno,car"), uma vez, e reaproveita
	 * entre todos os lotes do mesmo veiculo.
	 */
	private Map<String, List<Photo>> buildPhotoMap() {
		Map<String, List<Photo>> map = new LinkedHashMap<>();
		if (!downloadPhotos) {
			return map;
		}
		List<Vehicle> all = new ArrayList<>();
		Collections.addAll(all, CARS);
		Collections.addAll(all, MOTOS);
		int lock = 1;
		for (Vehicle vehicle : all) {
			String tag = brandTag(vehicle);
			List<Photo> photos = new ArrayList<>();
			for (int i = 0; i < PHOTOS_PER_ITEM; i++) {
				AuctionItemImage image = imageStorageService
						.download("https://loremflickr.com/640/420/" + tag + "?lock=" + (lock++));
				if (image != null && image.getBytes().length > 0) {
					photos.add(new Photo(image.getContentType(), image.getBytes()));
				}
			}
			map.put(vehicle.description(), photos);
		}
		LOG.info("Fotos reais carregadas para {} modelos de veiculo.", map.size());
		return map;
	}

	private String brandTag(Vehicle vehicle) {
		String brand = vehicle.description().split("/")[0].trim().toLowerCase().replace(" ", "");
		String kind = "motorcycle".equals(vehicle.type()) ? "motorcycle" : "car";
		return brand + "," + kind;
	}

	private AuctionItemImage svgImage(String description, String angle) {
		String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='480' height='320' viewBox='0 0 480 320'>"
				+ "<rect width='480' height='320' fill='#e2e8f0'/>"
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
