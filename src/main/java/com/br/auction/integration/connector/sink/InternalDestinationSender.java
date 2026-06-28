package com.br.auction.integration.connector.sink;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.integration.source.IntegrationSource;
import com.br.auction.integration.target.InternalTargetModel;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;
import com.br.auction.service.EditalService;
import com.br.auction.response.VehicleInfo;
import com.br.auction.service.ImageStorageService;
import com.br.auction.service.VehicleParserService;

/**
 * Grava um registro ja transformado nos modelos internos da aplicacao (destino fixo).
 *
 * <p>A aplicacao atual e sempre o destino: nao ha cadastro de destino nem de modelos
 * externos. Este componente faz o upsert direto em {@link Auction}/{@link AuctionItem}
 * usando a chave de negocio e estampa o provedor a partir da fonte configurada.</p>
 */
@Component
public class InternalDestinationSender {

	private static final DateTimeFormatter BRAZIL_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final AuctionRepository auctionRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final ImageStorageService imageStorageService;
	private final VehicleParserService vehicleParserService;
	private final EditalService editalService;

	public InternalDestinationSender(AuctionRepository auctionRepository,
			AuctionItemRepository auctionItemRepository, ImageStorageService imageStorageService,
			VehicleParserService vehicleParserService, EditalService editalService) {
		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.imageStorageService = imageStorageService;
		this.vehicleParserService = vehicleParserService;
		this.editalService = editalService;
	}

	/**
	 * Faz o upsert de um registro nos modelos internos dentro de uma transacao propria (por item),
	 * para que o acesso as colecoes LAZY (ex.: imagens) ocorra com sessao aberta e para que uma
	 * falha isole o rollback apenas daquele item. Excecoes sao propagadas para o executor marcar o
	 * item como falho (e desfazer eventuais escritas parciais).
	 */
	@Transactional
	public SendResult send(InternalTargetModel targetModel, IntegrationSource source, String businessKey,
			Map<String, Object> payload) {
		return switch (targetModel) {
			case AUCTION -> persistAuction(source, businessKey, payload);
			case AUCTION_ITEM -> persistAuctionItem(source, businessKey, payload);
		};
	}

	private SendResult persistAuction(IntegrationSource source, String businessKey, Map<String, Object> payload) {
		String detranAuctionId = textOr(payload.get("detranAuctionId"), businessKey);
		if (detranAuctionId == null || detranAuctionId.isBlank()) {
			return SendResult.failed("Registro de leilao sem detranAuctionId/chave de negocio");
		}
		String providerCode = resolveProviderCode(source);
		Optional<Auction> existing = auctionRepository.findByProviderCodeAndDetranAuctionId(providerCode,
				detranAuctionId);
		Auction auction = existing.orElseGet(Auction::new);
		boolean isNew = existing.isEmpty();

		auction.setDetranAuctionId(detranAuctionId);
		auction.setAuctionNoticeNumber(text(payload.get("auctionNoticeNumber")));
		auction.setCity(text(payload.get("city")));
		auction.setAuctioneer(text(payload.get("auctioneer")));
		auction.setStatus(text(payload.get("status")));
		auction.setClosingDate(parseDateTime(payload.get("closingDate")));
		auction.setAuctionYear(text(payload.get("auctionYear")));
		auction.setSourceUrl(text(payload.get("sourceUrl")));
		auction.setProviderCode(providerCode);
		auction.setProviderName(resolveProviderName(source));
		auction.setStateCode(source == null ? null : source.getStateCode());
		auction.setStateName(source == null ? null : source.getStateName());

		// Ja traz o edital (PDF publico) para a base na importacao; best-effort, nao quebra o item.
		editalService.populate(auction);

		auctionRepository.save(auction);
		return isNew ? SendResult.created() : SendResult.updated();
	}

	private SendResult persistAuctionItem(IntegrationSource source, String businessKey, Map<String, Object> payload) {
		String auctionDetranId = text(payload.get("auctionDetranId"));
		if (auctionDetranId == null || auctionDetranId.isBlank()) {
			return SendResult.failed("Item sem auctionDetranId para vincular ao leilao pai");
		}
		String lotId = textOr(payload.get("lotId"), businessKey);
		if (lotId == null || lotId.isBlank()) {
			return SendResult.failed("Item sem lotId/chave de negocio");
		}
		String providerCode = resolveProviderCode(source);
		Auction auction = auctionRepository.findByProviderCodeAndDetranAuctionId(providerCode, auctionDetranId)
				.orElseThrow(() -> new IllegalStateException(
						"Leilao pai nao encontrado para o provedor " + providerCode + " e id " + auctionDetranId));

		Optional<AuctionItem> existing = auctionItemRepository.findByAuctionIdAndLotId(auction.getId(), lotId);
		AuctionItem item = existing.orElseGet(AuctionItem::new);
		boolean isNew = existing.isEmpty();

		item.setAuction(auction);
		item.setLotId(lotId);
		item.setLotNumber(text(payload.get("lotNumber")));
		item.setLotType(text(payload.get("lotType")));
		String vehicleDescription = text(payload.get("vehicleDescription"));
		item.setVehicleDescription(vehicleDescription);
		VehicleInfo vehicleInfo = vehicleParserService.parse(vehicleDescription);
		item.setBrand(vehicleInfo.getBrand());
		item.setModel(vehicleInfo.getModel());
		item.setVehicleYear(vehicleInfo.getYear());
		// O HTML da lista so traz o LANCE INICIAL (piso), nunca o lance ao vivo. Por isso ele alimenta
		// o piso (minimumBidValue), e NAO sobrescreve o currentBidValue — este e mantido pelo refresh
		// ao vivo (LotLiveRefreshService). Em lote novo, semeamos currentBidValue com o inicial ate o
		// primeiro refresh chegar.
		BigDecimal initialBid = parseDecimal(payload.get("currentBidValue"));
		if (initialBid != null
				&& (item.getMinimumBidValue() == null || initialBid.compareTo(item.getMinimumBidValue()) < 0)) {
			item.setMinimumBidValue(initialBid);
		}
		if (item.getCurrentBidValue() == null) {
			item.setCurrentBidValue(initialBid);
		}
		item.setFipeValue(parseDecimal(payload.get("fipeValue")));
		imageStorageService.syncLotImages(item, toUrlList(payload.get("imageUrls")));

		auctionItemRepository.save(item);
		return isNew ? SendResult.created() : SendResult.updated();
	}

	private String resolveProviderCode(IntegrationSource source) {
		return source != null && source.getProviderCode() != null ? source.getProviderCode() : "UNKNOWN";
	}

	private String resolveProviderName(IntegrationSource source) {
		return source == null ? null : source.getProviderName();
	}

	private List<String> toUrlList(Object value) {
		List<String> urls = new ArrayList<>();
		if (value == null) {
			return urls;
		}
		if (value instanceof List<?> list) {
			for (Object element : list) {
				String url = text(element);
				if (url != null) {
					urls.add(url);
				}
			}
			return urls;
		}
		String single = text(value);
		if (single != null) {
			for (String part : single.split(",")) {
				String url = part.trim();
				if (!url.isEmpty()) {
					urls.add(url);
				}
			}
		}
		return urls;
	}

	private String text(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.isEmpty() ? null : text;
	}

	private String textOr(Object value, String fallback) {
		String text = text(value);
		return text != null ? text : fallback;
	}

	private BigDecimal parseDecimal(Object value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		String normalized = String.valueOf(value).replace("R$", "").trim();
		if (normalized.contains(",")) {
			normalized = normalized.replace(".", "").replace(",", ".");
		}
		if (normalized.isBlank()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private LocalDateTime parseDateTime(Object value) {
		String text = text(value);
		if (text == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(text, BRAZIL_DATE_TIME);
		} catch (RuntimeException ignored) {
			try {
				return LocalDateTime.parse(text);
			} catch (RuntimeException ex) {
				return null;
			}
		}
	}
}
