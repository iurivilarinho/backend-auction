package com.br.auction.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.LotType;
import com.br.auction.enums.PriceStatGroupBy;
import com.br.auction.filter.AuctionFilter;
import com.br.auction.filter.AuctionItemFacetFilter;
import com.br.auction.filter.AuctionItemFilter;
import com.br.auction.filter.PriceStatFilter;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;
import com.br.auction.response.AuctionItemFacetsResponse;
import com.br.auction.response.AuctionItemResponse;
import com.br.auction.response.AuctionListResponse;
import com.br.auction.response.FipeEnrichStatusResponse;
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
	private final DistanceService distanceService;
	private final GeocodingService geocodingService;
	private final EntityManager entityManager;

	/**
	 * Situacao das buscas FIPE em segundo plano, por leilao. Fica no servidor (nao no navegador), entao
	 * sobrevive a um F5 do front: a tela consulta {@code /fipe/status} e continua mostrando o progresso.
	 */
	private final Map<Long, FipeJobState> fipeJobs = new ConcurrentHashMap<>();

	public AuctionService(AuctionRepository auctionRepository, AuctionItemRepository auctionItemRepository,
			FipeService fipeService, EditalService editalService, DistanceService distanceService,
			GeocodingService geocodingService, EntityManager entityManager) {
		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.fipeService = fipeService;
		this.editalService = editalService;
		this.distanceService = distanceService;
		this.geocodingService = geocodingService;
		this.entityManager = entityManager;
	}

	/**
	 * Enfileira a geocodificacao das cidades distintas dos leiloes do escopo informado, garantindo
	 * antes que exista uma configuracao de origem. Devolve a quantidade de cidades enfileiradas.
	 */
	@Transactional
	public int warmupCities(Long auctionId, List<String> providerCodes, String stateCode) {
		distanceService.getOrCreateSettings();
		List<String[]> cities = distinctAuctionCities(auctionId, providerCodes, stateCode);
		for (String[] cityState : cities) {
			geocodingService.enqueue(cityState[0], cityState[1]);
		}
		return cities.size();
	}

	/**
	 * Preenche a distancia de cada leilao a partir da origem configurada, calculando uma unica vez
	 * por cidade distinta. Cidades ainda nao geocodificadas ficam com distancia nula e sao resolvidas
	 * em segundo plano, sem bloquear a listagem.
	 */
	@Transactional(readOnly = true)
	public void fillAuctionDistances(List<AuctionListResponse> auctions) {
		Map<String, Double> distancesByCity = new HashMap<>();
		for (AuctionListResponse auction : auctions) {
			if (auction.getCity() == null || auction.getCity().isBlank()) {
				continue;
			}
			String key = auction.getCity() + "|" + (auction.getStateCode() == null ? "" : auction.getStateCode());
			auction.setDistanceKm(distancesByCity.computeIfAbsent(key,
					k -> distanceService.distanceKm(auction.getCity(), auction.getStateCode())));
		}
	}

	/**
	 * Preenche a distancia de cada item de leilao a partir da origem configurada, reaproveitando o
	 * calculo por cidade distinta do leilao associado.
	 */
	@Transactional(readOnly = true)
	public void fillItemDistances(List<AuctionItemResponse> responses) {
		Map<String, Double> distancesByCity = new HashMap<>();
		for (AuctionItemResponse response : responses) {
			AuctionListResponse auction = response.getAuction();
			if (auction == null || auction.getCity() == null || auction.getCity().isBlank()) {
				continue;
			}
			String city = auction.getCity();
			String state = auction.getStateCode();
			String key = city + "|" + (state == null ? "" : state);
			response.setDistanceKm(distancesByCity.computeIfAbsent(key, k -> distanceService.distanceKm(city, state)));
		}
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
	public Page<Auction> findAll(AuctionFilter filter, Pageable page) {
		// providerCode/stateCode sao filtros opcionais: ausentes = todos os provedores/estados.
		// Aceita varios provedores (multi-selecao); nao usa provedor padrao para nao esconder a lista.
		return auctionRepository.findAll(AuctionSpecification.searchAllFields(filter.getSearch(), entityManager)
				.and(AuctionSpecification.statusEquals(filter.getStatus()))
				.and(AuctionSpecification.providerCodeIn(filter.getProviderCode()))
				.and(AuctionSpecification.stateCodeEquals(filter.getStateCode())), page);
	}

	@Transactional(readOnly = true)
	public Auction findById(Long auctionId) {
		return auctionRepository.findById(auctionId)
				.orElseThrow(() -> new EntityNotFoundException("Leilao nao encontrado para ID: " + auctionId));
	}

	@Transactional(readOnly = true)
	public Page<AuctionItem> findAllItems(AuctionItemFilter filter, Pageable page) {
		// providerCode/stateCode sao filtros opcionais (ausentes = todos). Sem provedor padrao: quando a
		// tela abre os itens por auctionId (sem provider), o proprio leilao ja delimita provedor/estado.
		return auctionItemRepository.findAll(AuctionItemSpecification.searchAllFields(filter.getSearch(), entityManager)
				.and(AuctionItemSpecification.auctionIdEquals(filter.getAuctionId()))
				.and(AuctionItemSpecification.typeEquals(filter.getType()))
				.and(AuctionItemSpecification.auctionStatusEquals(filter.getAuctionStatus()))
				.and(AuctionItemSpecification.brandIn(filter.getBrand()))
				.and(AuctionItemSpecification.yearIn(filter.getYear()))
				.and(AuctionItemSpecification.modelContains(filter.getModel()))
				.and(AuctionItemSpecification.bidBetween(filter.getMinBid(), filter.getMaxBid()))
				.and(AuctionItemSpecification.fipeBetween(filter.getMinFipe(), filter.getMaxFipe()))
				.and(AuctionItemSpecification.closedEquals(filter.getClosed()))
				.and(AuctionItemSpecification.providerCodeIn(filter.getProviderCode()))
				.and(AuctionItemSpecification.stateCodeEquals(filter.getStateCode())), page);
	}

	@Transactional(readOnly = true)
	public List<String> distinctBrands(Long auctionId, List<String> providerCodes, String stateCode) {
		List<String> codes = normalizeList(providerCodes);
		return auctionItemRepository.findDistinctBrands(auctionId, codes.isEmpty(), orPlaceholder(codes),
				blankToNull(stateCode));
	}

	@Transactional(readOnly = true)
	public List<String> distinctYears(Long auctionId, List<String> providerCodes, String stateCode) {
		List<String> codes = normalizeList(providerCodes);
		return auctionItemRepository.findDistinctYears(auctionId, codes.isEmpty(), orPlaceholder(codes),
				blankToNull(stateCode));
	}

	/** Marcas e anos distintos disponiveis para montar os filtros especializados, dado o escopo. */
	@Transactional(readOnly = true)
	public AuctionItemFacetsResponse findItemFacets(AuctionItemFacetFilter filter) {
		List<String> brands = distinctBrands(filter.getAuctionId(), filter.getProviderCode(), filter.getStateCode());
		List<String> years = distinctYears(filter.getAuctionId(), filter.getProviderCode(), filter.getStateCode());
		return new AuctionItemFacetsResponse(brands, years);
	}

	/** Calcula e persiste o valor FIPE de um item sob demanda (usa cache de 30 dias). */
	@Transactional
	public AuctionItem enrichItemFipe(Long itemId) {
		AuctionItem item = auctionItemRepository.findById(itemId)
				.orElseThrow(() -> new EntityNotFoundException("Item nao encontrado para ID: " + itemId));
		BigDecimal fipeValue = fipeService.getFipeValue(item.getVehicleDescription(), item.getVehicleYear());
		item.setFipeValue(fipeValue);
		return auctionItemRepository.save(item);
	}

	/**
	 * Marca o leilao como "em processamento" e dispara a busca FIPE assincrona. Se ja houver uma
	 * rodada em andamento para o mesmo leilao, nao inicia outra (evita processamento duplicado).
	 *
	 * @return {@code true} se iniciou uma nova rodada; {@code false} se ja havia uma em andamento.
	 */
	public boolean startAuctionFipeEnrichment(Long auctionId) {
		FipeJobState[] created = new FipeJobState[1];
		fipeJobs.compute(auctionId, (id, current) -> {
			if (current != null && current.running) {
				// ja existe rodada em andamento; mantem a atual e nao dispara outra
				return current;
			}
			FipeJobState fresh = new FipeJobState();
			fresh.running = true; // reserva a vaga sincronamente (evita disparo duplicado em corrida)
			created[0] = fresh;
			return fresh;
		});
		if (created[0] == null) {
			return false;
		}
		enrichAuctionItemsFipeAsync(auctionId);
		return true;
	}

	/** Situacao atual da busca FIPE de um leilao (para a tela reidratar o progresso apos um F5). */
	public FipeEnrichStatusResponse auctionFipeStatus(Long auctionId) {
		FipeJobState state = fipeJobs.get(auctionId);
		if (state == null) {
			return new FipeEnrichStatusResponse(false, 0, 0, 0, null);
		}
		String message = state.running
				? "Busca FIPE em andamento (" + state.processed.get() + "/" + state.total + ")."
				: "Busca FIPE concluida: " + state.enriched.get() + " item(ns) atualizado(s).";
		return new FipeEnrichStatusResponse(state.running, state.total, state.processed.get(), state.enriched.get(),
				message);
	}

	/**
	 * Enriquece em segundo plano o FIPE dos itens de um leilao que ainda nao tem valor, respeitando
	 * o cache. Roda assincrono para nao bloquear a requisicao; cada chamada externa e tolerante a falha.
	 * O progresso fica registrado em {@link #fipeJobs} para a tela poder consultar apos um refresh.
	 */
	@Async
	@Transactional
	public void enrichAuctionItemsFipeAsync(Long auctionId) {
		FipeJobState state = fipeJobs.computeIfAbsent(auctionId, id -> new FipeJobState());
		state.running = true;
		int enriched = 0;
		try {
			List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
			state.total = items.size();
			for (AuctionItem item : items) {
				state.processed.incrementAndGet();
				if (item.getFipeValue() != null && item.getFipeValue().compareTo(BigDecimal.ZERO) > 0) {
					continue;
				}
				try {
					BigDecimal fipeValue = fipeService.getFipeValue(item.getVehicleDescription(), item.getVehicleYear());
					if (fipeValue != null && fipeValue.compareTo(BigDecimal.ZERO) > 0) {
						item.setFipeValue(fipeValue);
						auctionItemRepository.save(item);
						enriched++;
						state.enriched.incrementAndGet();
					}
				} catch (RuntimeException ex) {
					LOG.debug("Falha ao buscar FIPE do item {}: {}", item.getId(), ex.getMessage());
				}
			}
			LOG.info("FIPE enriquecido para {} itens do leilao {}.", enriched, auctionId);
		} finally {
			state.running = false;
		}
	}

	/** Estado (em memoria, no servidor) de uma rodada de busca FIPE de um leilao. */
	private static final class FipeJobState {
		private volatile boolean running;
		private volatile int total;
		private final AtomicInteger processed = new AtomicInteger();
		private final AtomicInteger enriched = new AtomicInteger();
	}

	/**
	 * Estatisticas de preco de arremate sobre os itens com lances ENCERRADOS (leilao finalizado),
	 * agrupadas por marca / marca+modelo / marca+modelo+ano. O valor de arremate considerado e o
	 * {@code currentBidValue} (ultimo lance), que no estado finalizado representa o valor final.
	 */
	@Transactional(readOnly = true)
	public List<PriceStatResponse> priceStats(PriceStatFilter filter) {
		// providerCode/stateCode opcionais (ausentes = todos). Aceita varios provedores (multi-selecao).
		PriceStatGroupBy groupBy = filter.getGroupBy();
		// Preco medio ignora sucata (distorce a media); mantem conservados e itens sem classificacao
		// (ex.: MC, que nao rotula o lote) para nao zerar a estatistica desses provedores.
		List<AuctionItem> items = auctionItemRepository.findAll(AuctionItemSpecification.closedEquals(true)
				.and(AuctionItemSpecification.typeNotIn(List.of(LotType.SUCATA)))
				.and(AuctionItemSpecification.brandIn(filter.getBrand()))
				.and(AuctionItemSpecification.providerCodeIn(filter.getProviderCode()))
				.and(AuctionItemSpecification.stateCodeEquals(filter.getStateCode())));

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

	/** Normaliza filtro textual opcional: vazio/branco vira null (sem filtro). */
	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/** Normaliza lista de filtros textuais: remove nulos/brancos e faz trim. */
	private static List<String> normalizeList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
	}

	/** Garante lista nao-vazia para o parametro IN da query (o valor e ignorado quando "todos"). */
	private static List<String> orPlaceholder(List<String> codes) {
		return codes.isEmpty() ? List.of("") : codes;
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
	public List<String[]> distinctAuctionCities(Long auctionId, List<String> providerCodes, String stateCode) {
		List<String> codes = normalizeList(providerCodes);
		return auctionRepository.findDistinctCities(auctionId, codes.isEmpty(), orPlaceholder(codes),
				blankToNull(stateCode)).stream()
				.map(row -> new String[] { row[0] == null ? null : row[0].toString(),
						row[1] == null ? null : row[1].toString() })
				.toList();
	}
}
