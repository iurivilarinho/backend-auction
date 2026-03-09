package com.br.auction.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotResponse;

@Service
public class AuctionDetranService {

	private static final String URL = "https://leilao.detran.mg.gov.br/lotes/lista-lotes/passarIdAqui/2026?limit=2000";

	public AuctionJsonResponse fetchAuctionLots() throws IOException {

		Document document = Jsoup.connect(URL).userAgent("Mozilla/5.0").get();

		String auctionName = document.select("h4").text();
		String yardName = document.select("h6").text();

		List<LotResponse> lots = new ArrayList<>();

		Elements lotCards = document.select(".card.listaLotes");

		for (Element card : lotCards) {

			LotResponse lot = new LotResponse();

			lot.setAuctionName(auctionName);
			lot.setYardName(yardName);

			lot.setLotId(card.attr("id"));

			String lotNumber = card.select("span:contains(Lote)").first().text();
			lot.setLotNumber(lotNumber);

			String lotType = card.select("span:contains(CONSERVADO), span:contains(SUCATA)").text();
			lot.setLotType(lotType);

			String vehicleDescription = card.select("b:matchesOwn(^[A-Z])").last().text();
			lot.setVehicleDescription(vehicleDescription);

			String currentBid = card.select("[id^=valor_atual_lote]").text().replace("R$", "").trim();

			lot.setCurrentBidValue(currentBid);

			lots.add(lot);
		}

		int totalPages = 0;

		Elements pages = document.select(".pagination li a");

		for (Element page : pages) {
			try {
				int pageNumber = Integer.parseInt(page.text());
				if (pageNumber > totalPages) {
					totalPages = pageNumber;
				}
			} catch (Exception ignored) {
			}
		}

		AuctionJsonResponse response = new AuctionJsonResponse();
		response.setAuctionName(auctionName);
		response.setYardName(yardName);
		response.setTotalPages(totalPages);
		response.setLotsPerPage(lots.size());
		response.setLots(lots);

		return response;
	}

	private static final String URL_LIST = "https://leilao.detran.mg.gov.br/";

	public List<AuctionListJsonResponse> fetchAuctions() throws IOException {

		Document document = Jsoup.connect(URL_LIST).userAgent("Mozilla/5.0").get();

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

			auction.setAuctioneer(card.select(".row b").first().text());

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
}