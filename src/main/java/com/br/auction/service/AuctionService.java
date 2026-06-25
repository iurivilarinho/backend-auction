package com.br.auction.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;
import com.br.auction.response.PriceStatResponse;

import com.br.auction.specification.AuctionItemSpecification;
import com.br.auction.specification.AuctionSpecification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AuctionService {

	private static final Logger LOG = LoggerFactory.getLogger(AuctionService.class);

	private final AuctionRepository auctionRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final FipeService fipeService;
	private final EditalService editalService;
	private final EntityManager entityManager;

	public AuctionService(AuctionRepository auctionRepository, AuctionItemRepository auctionItemRepository,
			FipeService fipeService, EditalService editalService, EntityManager entityManager) {
		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.fipeService = fipeService;
		this.editalService = editalService;
		this.entityManager = entityManager;
	}

	/**
	 * Devolve o edital do leilao guardado na base; se ainda nao houver, baixa do DETRAN
	 * (PDF publico), guarda e devolve. {@code null} quando o leilao nao tem edital disponivel.
	 */
	@Transactional
	public Auction getOrFetchEdital(Long auctionId) {
		Auction auction = findById(auctionId);
		if (!auction.hasEdital() && editalService.populate(auction)) {
			auctionRepository.save(auction);
		}
		return auction.hasEdital() ? auction : null;
	}

	@Transactional(readOnly = true)
	public Page<Auction> findAll(List<AuctionStatus> status, String search, String providerCode, String stateCode,
			Pageable page) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;

		return auctionRepository.findAll(AuctionSpecification.searchAllFields(search, entityManager)
				.and(AuctionSpecification.statusEquals(status))
				.and(AuctionSpecification.providerCodeEquals(provider.getCode()))
				.and(AuctionSpecification.stateCodeEquals(resolvedStateCode)), page);
	}

	@Transactional(readOnly = true)
	public Auction findById(Long auctionId) {
		return auctionRepository.findById(auctionId)
				.orElseThrow(() -> new EntityNotFoundException("Leilao nao encontrado para ID: " + auctionId));
	}

	@Transactional(readOnly = true)
	public Page<AuctionItem> findAllItems(AuctionItemQuery query, Pageable page) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(query.providerCode());
		String resolvedStateCode = query.stateCode() == null || query.stateCode().isBlank() ? provider.getStateCode()
				: query.stateCode();

		return auctionItemRepository.findAll(AuctionItemSpecification.searchAllFields(query.search(), entityManager)
				.and(AuctionItemSpecification.auctionIdEquals(query.auctionId()))
				.and(AuctionItemSpecification.typeEquals(query.type()))
				.and(AuctionItemSpecification.auctionStatusEquals(query.auctionStatus()))
				.and(AuctionItemSpecification.brandIn(query.brands()))
				.and(AuctionItemSpecification.yearIn(query.years()))
				.and(AuctionItemSpecification.modelContains(query.model()))
				.and(AuctionItemSpecification.bidBetween(query.minBid(), query.maxBid()))
				.and(AuctionItemSpecification.fipeBetween(query.minFipe(), query.maxFipe()))
				.and(AuctionItemSpecification.closedEquals(query.closed()))
				.and(AuctionItemSpecification.providerCodeEquals(provider.getCode()))
				.and(AuctionItemSpecification.stateCodeEquals(resolvedStateCode)), page);
	}

	@Transactional(readOnly = true)
	public List<String> distinctBrands(Long auctionId, String providerCode, String stateCode) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;
		return auctionItemRepository.findDistinctBrands(auctionId, provider.getCode(), resolvedStateCode);
	}

	@Transactional(readOnly = true)
	public List<String> distinctYears(Long auctionId, String providerCode, String stateCode) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;
		return auctionItemRepository.findDistinctYears(auctionId, provider.getCode(), resolvedStateCode);
	}

	/** Calcula e persiste o valor FIPE de um item sob demanda (usa cache de 30 dias). */
	@Transactional
	public AuctionItem enrichItemFipe(Long itemId) {
		AuctionItem item = auctionItemRepository.findById(itemId)
				.orElseThrow(() -> new EntityNotFoundException("Item nao encontrado para ID: " + itemId));
		BigDecimal fipeValue = fipeService.getFipeValue(item.getVehicleDescription());
		item.setFipeValue(fipeValue);
		return auctionItemRepository.save(item);
	}

	/**
	 * Enriquece em segundo plano o FIPE dos itens de um leilao que ainda nao tem valor, respeitando
	 * o cache. Roda assincrono para nao bloquear a requisicao; cada chamada externa e tolerante a falha.
	 */
	@Async
	@Transactional
	public void enrichAuctionItemsFipeAsync(Long auctionId) {
		List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
		int enriched = 0;
		for (AuctionItem item : items) {
			if (item.getFipeValue() != null && item.getFipeValue().compareTo(BigDecimal.ZERO) > 0) {
				continue;
			}
			try {
				BigDecimal fipeValue = fipeService.getFipeValue(item.getVehicleDescription());
				if (fipeValue != null && fipeValue.compareTo(BigDecimal.ZERO) > 0) {
					item.setFipeValue(fipeValue);
					auctionItemRepository.save(item);
					enriched++;
				}
			} catch (RuntimeException ex) {
				LOG.debug("Falha ao buscar FIPE do item {}: {}", item.getId(), ex.getMessage());
			}
		}
		LOG.info("FIPE enriquecido para {} itens do leilao {}.", enriched, auctionId);
	}

	/**
	 * Estatisticas de preco de arremate sobre os itens com lances ENCERRADOS (leilao finalizado),
	 * agrupadas por marca / marca+modelo / marca+modelo+ano. O valor de arremate considerado e o
	 * {@code currentBidValue} (ultimo lance), que no estado finalizado representa o valor final.
	 */
	@Transactional(readOnly = true)
	public List<PriceStatResponse> priceStats(PriceStatGroupBy groupBy, List<String> brands, String providerCode,
			String stateCode) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;

		List<AuctionItem> items = auctionItemRepository.findAll(AuctionItemSpecification.closedEquals(true)
				.and(AuctionItemSpecification.brandIn(brands))
				.and(AuctionItemSpecification.providerCodeEquals(provider.getCode()))
				.and(AuctionItemSpecification.stateCodeEquals(resolvedStateCode)));

		Map<String, PriceAccumulator> groups = new java.util.LinkedHashMap<>();
		for (AuctionItem item : items) {
			BigDecimal bid = item.getCurrentBidValue();
			if (bid == null || bid.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			String brand = blankToDash(item.getBrand());
			String model = groupBy == PriceStatGroupBy.BRAND ? null : blankToDash(item.getModel());
			String year = groupBy == PriceStatGroupBy.BRAND_MODEL_YEAR ? blankToDash(item.getVehicleYear()) : null;
			String key = brand + "|" + (model == null ? "" : model) + "|" + (year == null ? "" : year);
			groups.computeIfAbsent(key, k -> new PriceAccumulator(brand, model, year)).add(bid);
		}

		return groups.values().stream()
				.map(PriceAccumulator::toResponse)
				.sorted(java.util.Comparator.comparingLong(PriceStatResponse::getCount).reversed())
				.toList();
	}

	private String blankToDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private static final class PriceAccumulator {
		private final String brand;
		private final String model;
		private final String year;
		private long count;
		private BigDecimal sum = BigDecimal.ZERO;
		private BigDecimal min;
		private BigDecimal max;

		private PriceAccumulator(String brand, String model, String year) {
			this.brand = brand;
			this.model = model;
			this.year = year;
		}

		private void add(BigDecimal bid) {
			count++;
			sum = sum.add(bid);
			min = min == null || bid.compareTo(min) < 0 ? bid : min;
			max = max == null || bid.compareTo(max) > 0 ? bid : max;
		}

		private PriceStatResponse toResponse() {
			BigDecimal average = count == 0 ? BigDecimal.ZERO
					: sum.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
			return new PriceStatResponse(brand, model, year, count, average, min, max);
		}
	}

	@Transactional(readOnly = true)
	public List<String[]> distinctAuctionCities(Long auctionId, String providerCode, String stateCode) {
		String normalizedProvider = providerCode == null || providerCode.isBlank() ? null : providerCode.trim();
		String normalizedState = stateCode == null || stateCode.isBlank() ? null : stateCode.trim();
		return auctionRepository.findDistinctCities(auctionId, normalizedProvider, normalizedState).stream()
				.map(row -> new String[] { row[0] == null ? null : row[0].toString(),
						row[1] == null ? null : row[1].toString() })
				.toList();
	}
}
