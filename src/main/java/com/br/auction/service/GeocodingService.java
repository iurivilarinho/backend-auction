package com.br.auction.service;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.br.auction.models.CityGeocode;
import com.br.auction.repository.CityGeocodeRepository;

/**
 * Geocodifica cidades (cidade + estado) em coordenadas usando o Nominatim/OpenStreetMap, sem
 * depender de chave de API. As coordenadas ficam em cache no banco; cidades ainda nao resolvidas
 * sao geocodificadas por um worker agendado que respeita o limite de uso (~1 req/s) do Nominatim.
 */
@Service
public class GeocodingService {

	private static final Logger LOG = LoggerFactory.getLogger(GeocodingService.class);

	private final CityGeocodeRepository repository;
	private final String nominatimUrl;
	private final String userAgent;
	private final RestClient restClient;
	private final Queue<String> pending = new ConcurrentLinkedQueue<>();

	public GeocodingService(CityGeocodeRepository repository,
			@Value("${auction.distance.nominatim-url:https://nominatim.openstreetmap.org/search}") String nominatimUrl,
			@Value("${auction.distance.user-agent:auction-app/1.0 (leilao backend)}") String userAgent) {
		this.repository = repository;
		this.nominatimUrl = nominatimUrl;
		this.userAgent = userAgent;
		this.restClient = RestClient.builder().build();
	}

	public Optional<CityGeocode> getCached(String city, String state) {
		if (city == null || city.isBlank() || state == null || state.isBlank()) {
			return Optional.empty();
		}
		return repository.findByCityIgnoreCaseAndStateIgnoreCase(city.trim(), state.trim());
	}

	/** Enfileira a cidade para geocodificacao assincrona, caso ainda nao esteja em cache. */
	public void enqueue(String city, String state) {
		if (city == null || city.isBlank() || state == null || state.isBlank()) {
			return;
		}
		if (getCached(city, state).isPresent()) {
			return;
		}
		String key = key(city, state);
		if (!pending.contains(key)) {
			pending.add(key);
		}
	}

	/** Geocodifica imediatamente (sincrono) e persiste o resultado. Usado para origem e warmup. */
	public CityGeocode geocodeNow(String city, String state) {
		Optional<CityGeocode> cached = getCached(city, state);
		if (cached.isPresent()) {
			return cached.get();
		}
		return resolveAndSave(city.trim(), state.trim());
	}

	@Scheduled(fixedDelayString = "${auction.distance.worker-delay-ms:1500}", initialDelayString = "30000")
	void processPending() {
		String key = pending.poll();
		if (key == null) {
			return;
		}
		String[] parts = key.split("\\|", 2);
		if (parts.length != 2) {
			return;
		}
		if (getCached(parts[0], parts[1]).isPresent()) {
			return;
		}
		try {
			resolveAndSave(parts[0], parts[1]);
		} catch (RuntimeException ex) {
			LOG.debug("Falha ao geocodificar {} - {}: {}", parts[0], parts[1], ex.getMessage());
		}
	}

	private CityGeocode resolveAndSave(String city, String state) {
		CityGeocode geocode = repository.findByCityIgnoreCaseAndStateIgnoreCase(city, state)
				.orElseGet(CityGeocode::new);
		geocode.setCity(city);
		geocode.setState(state);
		double[] coords = query(city, state);
		if (coords != null) {
			geocode.setLatitude(coords[0]);
			geocode.setLongitude(coords[1]);
			geocode.setResolved(true);
		} else {
			geocode.setResolved(false);
		}
		geocode.setUpdatedAt(java.time.LocalDateTime.now());
		return repository.save(geocode);
	}

	@SuppressWarnings("unchecked")
	private double[] query(String city, String state) {
		URI uri = UriComponentsBuilder.fromUriString(nominatimUrl)
				.queryParam("format", "json")
				.queryParam("limit", 1)
				.queryParam("country", "Brasil")
				.queryParam("state", state)
				.queryParam("city", city)
				.build()
				.toUri();
		Map<String, Object>[] results = restClient.get()
				.uri(uri)
				.header(HttpHeaders.USER_AGENT, userAgent)
				.header(HttpHeaders.ACCEPT, "application/json")
				.retrieve()
				.body(Map[].class);
		if (results == null || results.length == 0) {
			return null;
		}
		Map<String, Object> first = results[0];
		Object lat = first.get("lat");
		Object lon = first.get("lon");
		if (lat == null || lon == null) {
			return null;
		}
		try {
			return new double[] { Double.parseDouble(lat.toString()), Double.parseDouble(lon.toString()) };
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String key(String city, String state) {
		return city.trim().toLowerCase(Locale.ROOT) + "|" + state.trim().toUpperCase(Locale.ROOT);
	}
}
