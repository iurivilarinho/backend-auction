package com.br.auction.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.models.CityGeocode;
import com.br.auction.models.DistanceSetting;
import com.br.auction.repository.DistanceSettingRepository;

/**
 * Calcula a distancia (em linha reta, formula de Haversine) entre o ponto de origem configuravel
 * e a cidade de cada leilao. As coordenadas vem do {@link GeocodingService} (cache + Nominatim).
 * Quando a cidade de destino ainda nao foi geocodificada, retorna {@code null} e a enfileira para
 * resolucao assincrona, evitando bloquear a listagem.
 */
@Service
public class DistanceService {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private final DistanceSettingRepository settingRepository;
	private final GeocodingService geocodingService;
	private final String defaultOriginCity;
	private final String defaultOriginState;

	public DistanceService(DistanceSettingRepository settingRepository, GeocodingService geocodingService,
			@Value("${auction.distance.default-origin-city:Ituiutaba}") String defaultOriginCity,
			@Value("${auction.distance.default-origin-state:MG}") String defaultOriginState) {
		this.settingRepository = settingRepository;
		this.geocodingService = geocodingService;
		this.defaultOriginCity = defaultOriginCity;
		this.defaultOriginState = defaultOriginState;
	}

	@Transactional
	public DistanceSetting getOrCreateSettings() {
		return settingRepository.findById(DistanceSetting.SINGLETON_ID).orElseGet(() -> {
			DistanceSetting setting = new DistanceSetting();
			setting.setId(DistanceSetting.SINGLETON_ID);
			setting.setOriginCity(defaultOriginCity);
			setting.setOriginState(defaultOriginState);
			return settingRepository.save(setting);
		});
	}

	@Transactional
	public DistanceSetting updateSettings(String originCity, String originState) {
		DistanceSetting setting = getOrCreateSettings();
		setting.setOriginCity(originCity.trim());
		setting.setOriginState(originState.trim());
		// Origem por cidade: descarta o ponto exato do mapa para que a cidade volte a valer.
		setting.setOriginLatitude(null);
		setting.setOriginLongitude(null);
		setting.setOriginLabel(null);
		DistanceSetting saved = settingRepository.save(setting);
		// Garante que a origem esteja geocodificada para os calculos seguintes.
		geocodingService.geocodeNow(saved.getOriginCity(), saved.getOriginState());
		return saved;
	}

	/**
	 * Distancia em km entre a origem configurada e a cidade informada, ou {@code null} quando ainda
	 * nao ha coordenadas em cache (a cidade e enfileirada para geocodificacao assincrona).
	 */
	@Transactional
	public Double distanceKm(String city, String state) {
		if (city == null || city.isBlank()) {
			return null;
		}
		DistanceSetting setting = getOrCreateSettings();
		double[] origin = originCoordinates(setting);
		if (origin == null) {
			return null;
		}
		String resolvedState = state == null || state.isBlank() ? setting.getOriginState() : state;
		Optional<CityGeocode> destination = geocodingService.getCached(city, resolvedState);
		if (destination.isEmpty()) {
			geocodingService.enqueue(city, resolvedState);
			return null;
		}
		if (!destination.get().isResolved()) {
			return null;
		}
		return haversine(origin[0], origin[1],
				destination.get().getLatitude(), destination.get().getLongitude());
	}

	/**
	 * Coordenadas do ponto de origem: usa o ponto exato (lat/lng) definido no mapa quando existir;
	 * caso contrario geocodifica a cidade/estado configurados. Retorna {@code null} se nao resolver.
	 */
	private double[] originCoordinates(DistanceSetting setting) {
		if (setting.hasCoordinates()) {
			return new double[] { setting.getOriginLatitude(), setting.getOriginLongitude() };
		}
		Optional<CityGeocode> origin = geocodingService.getCached(setting.getOriginCity(), setting.getOriginState());
		if (origin.isEmpty()) {
			origin = Optional.of(geocodingService.geocodeNow(setting.getOriginCity(), setting.getOriginState()));
		}
		if (origin.isEmpty() || !origin.get().isResolved()) {
			return null;
		}
		return new double[] { origin.get().getLatitude(), origin.get().getLongitude() };
	}

	/** Define o ponto de origem por coordenadas (mapa). Limpa nada da cidade — mantida como rotulo. */
	@Transactional
	public DistanceSetting updateOriginPoint(double latitude, double longitude, String label, String city,
			String state) {
		DistanceSetting setting = getOrCreateSettings();
		setting.setOriginLatitude(latitude);
		setting.setOriginLongitude(longitude);
		setting.setOriginLabel(label == null || label.isBlank() ? null : label.trim());
		if (city != null && !city.isBlank()) {
			setting.setOriginCity(city.trim());
		}
		if (state != null && !state.isBlank()) {
			setting.setOriginState(state.trim());
		}
		return settingRepository.save(setting);
	}

	/** Geocodifica imediatamente a origem e a cidade informada (usado pelo warmup sob demanda). */
	public Double warmupAndDistance(String city, String state) {
		DistanceSetting setting = getOrCreateSettings();
		geocodingService.geocodeNow(setting.getOriginCity(), setting.getOriginState());
		if (city != null && !city.isBlank()) {
			String resolvedState = state == null || state.isBlank() ? setting.getOriginState() : state;
			geocodingService.geocodeNow(city, resolvedState);
		}
		return distanceKm(city, state);
	}

	private Double haversine(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return Math.round(EARTH_RADIUS_KM * c * 10.0) / 10.0;
	}
}
