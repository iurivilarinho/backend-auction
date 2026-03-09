package com.br.leilao.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.leilao.models.Auction;
import com.br.leilao.models.AuctionItem;
import com.br.leilao.repository.AuctionItemRepository;
import com.br.leilao.repository.AuctionRepository;
import com.br.leilao.response.AuctionListJsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuctionDetranImportService {

	private static final String URL_LIST = "https://leilao.detran.mg.gov.br/";
	private static final String URL_LOTS = "https://leilao.detran.mg.gov.br/lotes/lista-lotes/%s/%s?limit=2000";

	private final AuctionRepository auctionRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final FipeService fipeService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AuctionDetranImportService(AuctionRepository auctionRepository, AuctionItemRepository auctionItemRepository,
			FipeService fipeService) {

		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.fipeService = fipeService;
	}

	@Transactional
	@Scheduled(fixedDelay = 600000)
	public void importAllAuctions() {

		try {

			List<AuctionListJsonResponse> auctions = fetchAuctions();

			for (AuctionListJsonResponse auctionResponse : auctions) {

				if (auctionRepository.existsByDetranAuctionId(auctionResponse.getAuctionId())) {
					continue;
				}

				Auction auction = new Auction();

				auction.setAuctionNoticeNumber(auctionResponse.getAuctionNoticeNumber());
				auction.setCity(auctionResponse.getCity());
				auction.setAuctioneer(auctionResponse.getAuctioneer());
				auction.setStatus(auctionResponse.getStatus());
				auction.setDetranAuctionId(auctionResponse.getAuctionId());
				auction.setAuctionYear(auctionResponse.getAuctionYear());

				auctionRepository.save(auction);

				importAuctionLots(auction);
			}

		} catch (Exception e) {
			System.err.println("Erro ao importar leilões: " + e.getMessage());
		}
	}

	private void importAuctionLots(Auction auction) {

		try {

			String url = String.format(URL_LOTS, auction.getDetranAuctionId(), auction.getAuctionYear());

			Document document = Jsoup.connect(url).userAgent("Mozilla/5.0")
					.timeout((int) Duration.ofSeconds(30).toMillis()).get();

			Elements lotCards = document.select(".card.listaLotes");

			for (Element card : lotCards) {

				String lotId = card.attr("id");

				if (auctionItemRepository.existsByLotId(lotId)) {
					continue;
				}

				AuctionItem item = new AuctionItem();

				item.setAuction(auction);
				item.setLotId(lotId);

				Element lotSpan = card.selectFirst("span:contains(Lote)");
				if (lotSpan != null) {
					item.setLotNumber(lotSpan.text());
				}

				String lotType = card.select("span:contains(CONSERVADO), span:contains(SUCATA)").text();
				item.setLotType(lotType);

				Element desc = card.select("b:matchesOwn(^[A-Z])").last();
				if (desc != null) {
					item.setVehicleDescription(desc.text());
				}

				String bid = card.select("[id^=valor_atual_lote]").text().replace("R$", "").replace(".", "")
						.replace(",", ".").trim();

				if (!bid.isEmpty()) {
					item.setCurrentBidValue(new BigDecimal(bid));
				}

				BigDecimal fipeValue = fipeService.getFipeValue(item.getVehicleDescription());
				item.setFipeValue(fipeValue);

				auctionItemRepository.save(item);
			}

		} catch (Exception e) {
			System.err.println(
					"Erro ao importar lotes do leilão " + auction.getDetranAuctionId() + ": " + e.getMessage());
		}
	}

	public List<AuctionListJsonResponse> fetchAuctions() throws IOException {

		Document document = Jsoup.connect(URL_LIST).userAgent("Mozilla/5.0").timeout(30000).get();

		List<AuctionListJsonResponse> auctions = new ArrayList<>();

		Elements cards = document.select(".col-md-3 .card");

		for (Element card : cards) {

			Element link = card.selectFirst("a[href*=/lotes/lista-lotes/]");

			if (link == null) {
				continue;
			}

			AuctionListJsonResponse auction = new AuctionListJsonResponse();

			String notice = card.select(".card-title").text();
			auction.setAuctionNoticeNumber(notice.replace("Edital de Leilão", "").trim());

			auction.setCity(card.select(".capa-municipio").text());

			Element auctioneer = card.select(".row b").first();
			if (auctioneer != null) {
				auction.setAuctioneer(auctioneer.text());
			}

			auction.setStatus(card.select(".text-primary, .text-success, .text-danger").text());

			String closingDate = card.select("div:contains(Encerramento)").text().replace("Encerramento:", "").trim();

			auction.setClosingDate(closingDate);

			String href = link.attr("href");
			String[] parts = href.split("/");

			auction.setAuctionId(parts[3]);
			auction.setAuctionYear(parts[4]);

			auctions.add(auction);
		}

		return auctions;
	}

	@Scheduled(fixedDelay = 180000)
	public void updateValuesAuctionItems() {

		System.err.println("Atualizando valores de Items em andamento....");

		try {

			List<AuctionItem> items = auctionItemRepository.findAll();

			if (items.isEmpty()) {
				return;
			}

			int batchSize = 100;

			for (int i = 0; i < items.size(); i += batchSize) {

				int end = Math.min(i + batchSize, items.size());

				List<AuctionItem> batch = items.subList(i, end);

				processBatch(batch);
			}

		} catch (Exception e) {

			System.err.println("Erro ao atualizar valores dos lotes: " + e.getMessage());

		}
	}

	private void processBatch(List<AuctionItem> batch) {

		try {

			StringBuilder urlBuilder = new StringBuilder(
					"https://leilao.detran.mg.gov.br/PDO/updateCountdown.php?user=");

			for (AuctionItem item : batch) {
				urlBuilder.append("&data[]=").append(item.getLotId());
			}

			String url = urlBuilder.toString();

			String response = Jsoup.connect(url).ignoreContentType(true).userAgent("Mozilla/5.0").timeout(30000)
					.execute().body();

			Map<String, Object> json = objectMapper.readValue(response, Map.class);

			for (AuctionItem item : batch) {

				Object lotObj = json.get(item.getLotId());

				if (lotObj == null) {
					continue;
				}

				Map<String, Object> lotData = (Map<String, Object>) lotObj;

				Object valueObj = lotData.get("valor");

				if (valueObj == null) {
					continue;
				}

				String value = valueObj.toString();

				BigDecimal newValue = new BigDecimal(value.replace("R$", "").replace(".", "").replace(",", ".").trim());

				item.setCurrentBidValue(newValue);
			}

			auctionItemRepository.saveAll(batch);

		} catch (Exception e) {

			System.err.println("Erro ao atualizar batch: " + e.getMessage());

		}
	}
}