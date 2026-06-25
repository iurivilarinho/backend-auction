package com.br.auction.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.response.AuctionJsonResponse;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotResponse;

@Service
public class AuctionDetranService {

	private static final int LOTS_PAGE_SIZE = 100;
	private static volatile boolean permissiveTlsConfigured;

	private final String userAgent;
	private final int timeoutMs;
	private final boolean validateTlsCertificates;

	public AuctionDetranService(@Value("${auction.source.user-agent}") String userAgent,
			@Value("${auction.source.timeout-ms}") int timeoutMs,
			@Value("${auction.source.validate-tls-certificates}") boolean validateTlsCertificates) {
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
		this.validateTlsCertificates = validateTlsCertificates;
	}

	public AuctionJsonResponse fetchAuctionLots() throws IOException {
		AuctionProvider provider = AuctionProvider.defaultProvider();
		return fetchAuctionLots(provider, "", "");
	}

	public AuctionJsonResponse fetchAuctionLots(AuctionProvider provider, String auctionId, String auctionYear)
			throws IOException {
		List<LotResponse> lots = new ArrayList<>();
		String auctionName = "";
		String yardName = "";
		int totalPages = 1;
		int currentPage = 1;

		while (currentPage <= totalPages) {
			Document document = fetchLotsPage(provider, auctionId, auctionYear, currentPage);

			if (auctionName.isBlank()) {
				auctionName = document.select("h4").text();
			}
			if (yardName.isBlank()) {
				yardName = document.select("h6").text();
			}

			lots.addAll(parseLots(document, auctionName, yardName));
			totalPages = Math.max(totalPages, resolveTotalPages(document));
			currentPage++;
		}

		AuctionJsonResponse response = new AuctionJsonResponse();
		response.setAuctionName(auctionName);
		response.setYardName(yardName);
		response.setTotalPages(totalPages);
		response.setLotsPerPage(lots.size());
		response.setLots(lots);
		return response;
	}

	public List<AuctionListJsonResponse> fetchAuctions() throws IOException {
		return fetchAuctions(AuctionProvider.defaultProvider(), new AuctionSourceFilter());
	}

	public List<AuctionListJsonResponse> fetchAuctions(AuctionProvider provider, AuctionSourceFilter filter)
			throws IOException {
		Document document = filter != null && filter.hasAnySourceFilter()
				? fetchFilteredAuctionList(provider, filter)
				: fetchAuctionList(provider);

		return parseAuctionCards(provider, document);
	}

	private Document fetchAuctionList(AuctionProvider provider) throws IOException {
		return connect(provider.getBaseUrl())
				.get();
	}

	private Document fetchFilteredAuctionList(AuctionProvider provider, AuctionSourceFilter filter) throws IOException {
		Connection.Response getResponse = connect(provider.getBaseUrl())
				.method(Connection.Method.GET)
				.execute();

		Document formDocument = getResponse.parse();
		Map<String, String> payload = buildFilterPayload(formDocument, filter);

		return connect(provider.getBaseUrl())
				.referrer(provider.getBaseUrl())
				.cookies(getResponse.cookies())
				.data(payload)
				.method(Connection.Method.POST)
				.execute()
				.parse();
	}

	private Document fetchLotsPage(AuctionProvider provider, String auctionId, String auctionYear, int page)
			throws IOException {
		String url = provider.getBaseUrl() + "/lotes/lista-lotes/" + auctionId + "/" + auctionYear + "?page=" + page
				+ "&limit=" + LOTS_PAGE_SIZE;
		return connect(url)
				.get();
	}

	private Connection connect(String url) {
		configureTlsIfNeeded();
		return Jsoup.connect(url)
				.userAgent(userAgent)
				.timeout(timeoutMs);
	}

	private void configureTlsIfNeeded() {
		if (validateTlsCertificates || permissiveTlsConfigured) {
			return;
		}

		synchronized (AuctionDetranService.class) {
			if (permissiveTlsConfigured) {
				return;
			}

			try {
				TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) {
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) {
					}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}
				} };
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, trustManagers, new SecureRandom());
				SSLContext.setDefault(sslContext);
				permissiveTlsConfigured = true;
			} catch (GeneralSecurityException ex) {
				throw new IllegalStateException("Nao foi possivel configurar TLS para consulta da fonte externa.", ex);
			}
		}
	}

	private Map<String, String> buildFilterPayload(Document document, AuctionSourceFilter filter) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("_method", "POST");
		payload.put("Leiloes[numero]", valueOrEmpty(filter.getAuctionNumber()));
		payload.put("Leiloes[data_fechamento_ini]", valueOrEmpty(filter.getClosingDateStart()));
		payload.put("Leiloes[data_fechamento_fim]", valueOrEmpty(filter.getClosingDateEnd()));
		payload.put("Leiloes[municipio_id]", valueOrEmpty(filter.getMunicipalityId()));
		payload.put("Leiloes[tipo_veiculo]", valueOrEmpty(filter.getVehicleTypeCode()));
		payload.put("Leiloes[marca]", valueOrEmpty(filter.getBrand()));
		payload.put("Leiloes[modelo]", valueOrEmpty(filter.getModel()));
		payload.put("Leiloes[ano_veiculo]", valueOrEmpty(filter.getVehicleYear()));
		payload.put("Leiloes[cor_veiculo]", valueOrEmpty(filter.getColor()));
		payload.put("Leiloes[condicao_veiculo]", valueOrEmpty(filter.getCondition()));
		payload.put("_Token[fields]", findInputValue(document, "_Token[fields]"));
		payload.put("_Token[unlocked]", findInputValue(document, "_Token[unlocked]"));
		return payload;
	}

	private List<AuctionListJsonResponse> parseAuctionCards(AuctionProvider provider, Document document) {
		List<AuctionListJsonResponse> auctions = new ArrayList<>();
		Elements cards = document.select(".col-md-3 .card");

		for (Element card : cards) {
			Element link = card.selectFirst("a[href*=/lotes/lista-lotes/]");
			if (link == null) {
				continue;
			}

			String href = link.attr("href");
			String[] parts = href.split("/");
			if (parts.length < 5) {
				continue;
			}

			AuctionListJsonResponse auction = new AuctionListJsonResponse();
			String notice = card.select(".card-title").text();
			auction.setAuctionNoticeNumber(notice.replace("Edital de Leilao", "").replace("Edital de Leilão", "").trim());
			auction.setCity(card.select(".capa-municipio").text());

			Element auctioneer = card.select(".row b").first();
			if (auctioneer != null) {
				auction.setAuctioneer(auctioneer.text().trim());
			}

			auction.setStatus(card.select(".text-primary, .text-success, .text-danger").text().trim());
			auction.setClosingDate(card.select("div:contains(Encerramento)").text().replace("Encerramento:", "").trim());
			auction.setAuctionId(parts[3]);
			auction.setAuctionYear(parts[4]);
			auction.setSourceUrl(provider.getBaseUrl() + href);
			auctions.add(auction);
		}

		return auctions;
	}

	private List<LotResponse> parseLots(Document document, String auctionName, String yardName) {
		List<LotResponse> lots = new ArrayList<>();
		Elements lotCards = document.select(".card.listaLotes");

		for (Element card : lotCards) {
			LotResponse lot = new LotResponse();
			lot.setAuctionName(auctionName);
			lot.setYardName(yardName);
			lot.setLotId(card.attr("id"));

			Element lotNumber = card.selectFirst("span:contains(Lote)");
			if (lotNumber != null) {
				lot.setLotNumber(lotNumber.text());
			}

			lot.setLotType(card.select("span:contains(CONSERVADO), span:contains(SUCATA)").text());

			Element vehicleDescription = card.select("b:matchesOwn(^[A-Z])").last();
			if (vehicleDescription != null) {
				lot.setVehicleDescription(vehicleDescription.text());
			}

			String currentBid = card.select("[id^=valor_atual_lote]").text().replace("R$", "").trim();
			lot.setCurrentBidValue(currentBid);
			lot.setImageUrls(extractImageUrls(card));
			lots.add(lot);
		}

		return lots;
	}

	private List<String> extractImageUrls(Element card) {
		List<String> urls = new ArrayList<>();
		for (Element image : card.select("img")) {
			String url = firstNonBlank(image.absUrl("data-src"), image.absUrl("src"), image.attr("data-src"),
					image.attr("src"));
			if (url != null && !urls.contains(url)) {
				urls.add(url);
			}
		}
		return urls;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private int resolveTotalPages(Document document) {
		int totalPages = 1;
		Elements pages = document.select(".pagination li a");

		for (Element page : pages) {
			try {
				int pageNumber = Integer.parseInt(page.text().trim());
				if (pageNumber > totalPages) {
					totalPages = pageNumber;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		return totalPages;
	}

	private String findInputValue(Document document, String name) {
		for (Element input : document.select("input")) {
			if (name.equals(input.attr("name"))) {
				return input.attr("value");
			}
		}
		return "";
	}

	private String valueOrEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
