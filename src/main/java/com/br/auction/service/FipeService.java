package com.br.auction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.br.auction.models.VehicleFipeCache;
import com.br.auction.repository.VehicleFipeCacheRepository;
import com.br.auction.response.VehicleInfo;

/**
 * Consulta o valor da tabela FIPE usando a API publica parallelum (v2). A v2 nao exige token e e
 * mais tolerante a taxa que a v1. O resultado fica em cache por 30 dias (chave marca+modelo+ano).
 *
 * <p>O casamento entre a descricao do veiculo (provedor) e os nomes da FIPE e feito por
 * sobreposicao de tokens: tenta-se a marca por similaridade e os melhores modelos candidatos
 * (varias geracoes) ate encontrar um que tenha o ano informado.</p>
 */
@Service
public class FipeService {

	// Tipos da API v2 (ingles).
	private static final String[] FIPE_TYPES = { "cars", "motorcycles", "trucks" };
	private static final int MAX_MODEL_CANDIDATES = 4;
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0 Safari/537.36";

	private final VehicleParserService parser;
	private final VehicleFipeCacheRepository cacheRepository;
	private final RestTemplate restTemplate = new RestTemplate();
	private final String baseUrlTemplate;
	private final String token;
	// Cache em memoria das marcas por tipo: evita repetir a chamada de marcas a cada consulta
	// (reduz o numero de requisicoes a fonte FIPE).
	private final Map<String, Map[]> brandsByType = new ConcurrentHashMap<>();

	public FipeService(VehicleParserService parser, VehicleFipeCacheRepository cacheRepository,
			@Value("${fipe.api.base-url:https://parallelum.com.br/fipe/api/v2}") String baseUrlTemplate,
			@Value("${fipe.api.token:}") String token) {
		this.parser = parser;
		this.cacheRepository = cacheRepository;
		this.baseUrlTemplate = baseUrlTemplate.endsWith("/")
				? baseUrlTemplate.substring(0, baseUrlTemplate.length() - 1)
				: baseUrlTemplate;
		this.token = token;
		this.restTemplate.getInterceptors().add((request, body, execution) -> {
			request.getHeaders().add(HttpHeaders.USER_AGENT, USER_AGENT);
			return execution.execute(request, body);
		});
	}

	public BigDecimal getFipeValue(String vehicleDescription) {
		return getFipeValue(vehicleDescription, null);
	}

	/**
	 * Igual ao {@link #getFipeValue(String)}, mas aceita um ano de reserva vindo de um campo proprio do
	 * item (ex.: {@code vehicleYear}). Provedores como o LEILO nao colocam o ano na descricao, entao o
	 * parser devolvia ano nulo e o casamento FIPE falhava sempre (a FIPE exige o ano). Com o fallback,
	 * o ano do item entra quando a descricao nao traz um.
	 */
	public BigDecimal getFipeValue(String vehicleDescription, String fallbackYear) {

		VehicleInfo vehicle = parser.parse(vehicleDescription);

		if (vehicle.getBrand() == null || vehicle.getModel() == null) {
			return BigDecimal.ZERO;
		}

		if ((vehicle.getYear() == null || vehicle.getYear().isBlank()) && fallbackYear != null
				&& !fallbackYear.isBlank()) {
			vehicle.setYear(fallbackYear.trim());
		}

		Optional<VehicleFipeCache> cacheOpt = cacheRepository.findByBrandAndModelAndYear(vehicle.getBrand(),
				vehicle.getModel(), vehicle.getYear());

		if (cacheOpt.isPresent()) {

			VehicleFipeCache cache = cacheOpt.get();

			if (cache.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))) {

				return cache.getFipeValue();
			}

			BigDecimal newValue = queryFipe(vehicle);
			if (newValue.compareTo(BigDecimal.ZERO) <= 0) {
				// Falha transitoria (ex.: limite da API): mantem o valor anterior do cache.
				return cache.getFipeValue();
			}

			cache.setFipeValue(newValue);
			cache.setCreatedAt(LocalDateTime.now());

			cacheRepository.save(cache);

			return newValue;
		}

		BigDecimal fipeValue = queryFipe(vehicle);

		if (fipeValue.compareTo(BigDecimal.ZERO) > 0) {

			VehicleFipeCache cache = new VehicleFipeCache();

			cache.setBrand(vehicle.getBrand());
			cache.setModel(vehicle.getModel());
			cache.setYear(vehicle.getYear());
			cache.setVehicleType(vehicle.getVehicleType());
			cache.setFipeValue(fipeValue);
			cache.setCreatedAt(LocalDateTime.now());

			cacheRepository.save(cache);
		}

		return fipeValue;
	}

	private BigDecimal queryFipe(VehicleInfo vehicle) {

		String brandNorm = normalize(vehicle.getBrand());
		List<String> modelTokens = tokenize(vehicle.getModel());
		String year = vehicle.getYear() == null ? null : vehicle.getYear().replaceAll("\\D", "");
		if (year != null && year.length() > 4) {
			// ex.: "2012/2013" -> "20122013" -> mantem so o ano-modelo (primeiros 4 digitos)
			year = year.substring(0, 4);
		}

		for (String type : FIPE_TYPES) {

			try {

				String baseUrl = baseUrlTemplate + "/" + type;

				Map[] brands = brandsByType.computeIfAbsent(type,
						key -> restTemplate.getForObject(withToken(baseUrl + "/brands"), Map[].class));
				String brandId = bestMatchCode(brands, brandNorm);
				if (brandId == null) {
					continue;
				}

				Map[] models = restTemplate.getForObject(withToken(baseUrl + "/brands/" + brandId + "/models"),
						Map[].class);
				if (models == null) {
					continue;
				}
				// Varios modelos podem casar (geracoes diferentes). Tenta os melhores candidatos em
				// ordem ate encontrar um que tenha o ano informado.
				for (String modelId : topModelCodes(models, modelTokens)) {
					Map[] years = restTemplate.getForObject(
							withToken(baseUrl + "/brands/" + brandId + "/models/" + modelId + "/years"), Map[].class);
					String yearId = bestYearCode(years, year);
					if (yearId == null) {
						continue;
					}
					Map value = restTemplate.getForObject(
							withToken(baseUrl + "/brands/" + brandId + "/models/" + modelId + "/years/" + yearId),
							Map.class);
					if (value == null || value.get("price") == null) {
						continue;
					}
					String price = value.get("price").toString().replace("R$", "").replace(".", "").replace(",", ".")
							.trim();
					vehicle.setVehicleType(type);
					return new BigDecimal(price);
				}

			} catch (Exception ex) {
				brandsByType.remove(type);
				org.slf4j.LoggerFactory.getLogger(FipeService.class)
						.warn("FIPE tipo={} marca={} modelo={} ano={} falhou: {}", type, vehicle.getBrand(),
								vehicle.getModel(), vehicle.getYear(), ex.toString());
			}
		}

		return BigDecimal.ZERO;
	}

	/** Marca: escolhe o codigo cujo nome normalizado tem maior sobreposicao com a marca informada. */
	private String bestMatchCode(Map[] brands, String brandNorm) {
		if (brands == null || brandNorm == null || brandNorm.isBlank()) {
			return null;
		}
		String code = null;
		int best = 0;
		for (Map brand : brands) {
			String name = normalize(String.valueOf(brand.get("name")));
			int score = 0;
			if (name.contains(brandNorm) || brandNorm.contains(name)) {
				score = Math.max(brandNorm.length(), name.length());
			} else {
				for (String token : name.split("\\s+|-|/")) {
					if (token.length() >= 2 && (brandNorm.contains(token) || token.contains(brandNorm))) {
						score = Math.max(score, token.length());
					}
				}
			}
			if (score > best) {
				best = score;
				code = String.valueOf(brand.get("code"));
			}
		}
		return code;
	}

	/**
	 * Modelo: devolve os codigos dos modelos com maior sobreposicao de tokens, em ordem decrescente
	 * de score. Retornar varios candidatos permite tentar geracoes diferentes ate achar o ano certo.
	 */
	private List<String> topModelCodes(Map[] models, List<String> modelTokens) {
		if (models.length == 0 || modelTokens.isEmpty()) {
			return List.of();
		}
		record Candidate(String code, int score) {
		}
		List<Candidate> candidates = new java.util.ArrayList<>();
		for (Map model : models) {
			String name = normalize(String.valueOf(model.get("name")));
			int score = 0;
			for (String token : modelTokens) {
				if (name.contains(token)) {
					score++;
				}
			}
			if (score > 0) {
				candidates.add(new Candidate(String.valueOf(model.get("code")), score));
			}
		}
		return candidates.stream()
				.sorted(java.util.Comparator.comparingInt(Candidate::score).reversed())
				.limit(MAX_MODEL_CANDIDATES)
				.map(Candidate::code)
				.toList();
	}

	/** Anexa o token configurado (quando houver) para fontes FIPE que exigem autenticacao. */
	private String withToken(String url) {
		if (token == null || token.isBlank()) {
			return url;
		}
		return url + (url.contains("?") ? "&" : "?") + "token=" + token;
	}

	/** Ano: escolhe o codigo do ano que contem os 4 digitos do ano informado. */
	private String bestYearCode(Map[] years, String year) {
		if (years == null || years.length == 0) {
			return null;
		}
		if (year != null && year.length() == 4) {
			for (Map y : years) {
				if (String.valueOf(y.get("name")).contains(year) || String.valueOf(y.get("code")).startsWith(year)) {
					return String.valueOf(y.get("code"));
				}
			}
		}
		return null;
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toUpperCase()
				.trim();
	}

	private List<String> tokenize(String value) {
		List<String> tokens = new java.util.ArrayList<>();
		if (value == null) {
			return tokens;
		}
		for (String token : normalize(value).split("\\s+|/|-")) {
			if (token.length() >= 2) {
				tokens.add(token);
			}
		}
		return tokens;
	}
}
