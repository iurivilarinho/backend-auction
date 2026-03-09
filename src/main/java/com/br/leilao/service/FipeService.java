package com.br.leilao.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.br.leilao.models.VehicleFipeCache;
import com.br.leilao.repository.VehicleFipeCacheRepository;
import com.br.leilao.response.VehicleInfo;

@Service
public class FipeService {

	private static final String[] FIPE_TYPES = { "carros", "motos", "caminhoes" };

	private final VehicleParserService parser;
	private final VehicleFipeCacheRepository cacheRepository;
	private final RestTemplate restTemplate = new RestTemplate();

	public FipeService(VehicleParserService parser, VehicleFipeCacheRepository cacheRepository) {

		this.parser = parser;
		this.cacheRepository = cacheRepository;
	}

	public BigDecimal getFipeValue(String vehicleDescription) {

		VehicleInfo vehicle = parser.parse(vehicleDescription);

		if (vehicle.getBrand() == null || vehicle.getModel() == null) {
			return BigDecimal.ZERO;
		}

		Optional<VehicleFipeCache> cacheOpt = cacheRepository.findByBrandAndModelAndYear(vehicle.getBrand(),
				vehicle.getModel(), vehicle.getYear());

		if (cacheOpt.isPresent()) {

			VehicleFipeCache cache = cacheOpt.get();

			if (cache.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))) {

				return cache.getFipeValue();
			}

			BigDecimal newValue = queryFipe(vehicle);

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

		for (String type : FIPE_TYPES) {

			try {

				String baseUrl = "https://parallelum.com.br/fipe/api/v1/" + type;

				Map[] brands = restTemplate.getForObject(baseUrl + "/marcas", Map[].class);

				String brandId = null;

				for (Map b : brands) {

					if (b.get("nome").toString().toUpperCase().contains(vehicle.getBrand())) {

						brandId = b.get("codigo").toString();
						break;
					}
				}

				if (brandId == null) {
					continue;
				}

				Map modelsResponse = restTemplate.getForObject(baseUrl + "/marcas/" + brandId + "/modelos", Map.class);

				Map[] models = (Map[]) modelsResponse.get("modelos");

				String modelId = null;

				for (Map m : models) {

					if (m.get("nome").toString().toUpperCase().contains(vehicle.getModel())) {

						modelId = m.get("codigo").toString();
						break;
					}
				}

				if (modelId == null) {
					continue;
				}

				Map[] years = restTemplate
						.getForObject(baseUrl + "/marcas/" + brandId + "/modelos/" + modelId + "/anos", Map[].class);

				String yearId = null;

				for (Map y : years) {

					if (y.get("nome").toString().contains(vehicle.getYear())) {

						yearId = y.get("codigo").toString();
						break;
					}
				}

				if (yearId == null) {
					continue;
				}

				Map value = restTemplate.getForObject(
						baseUrl + "/marcas/" + brandId + "/modelos/" + modelId + "/anos/" + yearId, Map.class);

				String price = value.get("Valor").toString().replace("R$", "").replace(".", "").replace(",", ".")
						.trim();

				vehicle.setVehicleType(type);

				return new BigDecimal(price);

			} catch (Exception ignored) {
			}
		}

		return BigDecimal.ZERO;
	}
}