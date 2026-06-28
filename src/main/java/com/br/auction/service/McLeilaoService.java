package com.br.auction.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.response.AuctionListJsonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adapter do provedor MC LEILAO (mcleilao.com.br, GO). SPA Angular com API publica em
 * {@code api.mcleilaoeireli.com.br} (sem token, UA de navegador):
 *
 * <ul>
 *   <li>POST {@code /leilao/listarLeiloes} body {@code {status:[...],maximoRetorno,ordenacao,posicao}} — leiloes.</li>
 *   <li>GET {@code /loteGeral/buscartListaLotePorLeilaoId?idLeilao=ID} — ids dos lotes do leilao.</li>
 *   <li>GET {@code /loteGeral/buscarLotePorId?idLote=ID} — detalhe do lote (lance, prazo, veiculos[]).</li>
 * </ul>
 *
 * Filtra para VEICULOS (lote com {@code veiculos} nao-vazio). Status validos: ENCERRADO, RETIRADO,
 * LOTEAMENTO, ANDAMENTO, SUSPENSO. Veiculo nao tem token; o "orgaos-only" segue o flag compartilhado.
 */
@Service
public class McLeilaoService {

	private static final Logger LOG = LoggerFactory.getLogger(McLeilaoService.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
	private static final List<String> STATUSES = List.of("ANDAMENTO", "SUSPENSO", "LOTEAMENTO", "ENCERRADO");
	private static final List<String> ORGAO_KEYWORDS = List.of("detran", "prf", "policia", "polícia", "receita",
			"prefeitura", "municipio", "município", "governo", "estado do", "comarca", "tribunal", "justica", "justiça",
			"vara ", "rodoviaria", "rodoviária");

	private final String userAgent;
	private final int timeoutMs;
	private final boolean orgaosOnly;

	public McLeilaoService(
			@Value("${lot.leilo.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36}") String userAgent,
			@Value("${lot.leilo.timeout-ms:30000}") int timeoutMs,
			@Value("${lot.leilo.orgaos-only:false}") boolean orgaosOnly) {
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
		this.orgaosOnly = orgaosOnly;
	}

	/** Leiloes (listarLeiloes) mapeados para o formato de feed. */
	public List<AuctionListJsonResponse> fetchAuctions(AuctionProvider provider) {
		List<AuctionListJsonResponse> out = new ArrayList<>();
		JsonNode arr = listarLeiloes(provider);
		if (arr == null || !arr.isArray()) {
			return out;
		}
		for (JsonNode a : arr) {
			AuctionListJsonResponse r = new AuctionListJsonResponse();
			r.setAuctionId(text(a, "id"));
			r.setAuctionNoticeNumber(text(a, "descricao"));
			r.setCity(text(a, "endereco"));
			r.setAuctioneer(a.path("comitente").path("nome").asText(null));
			r.setStatus(text(a, "status"));
			r.setClosingDate(brDate(text(a, "data")));
			r.setSourceUrl(firstNonBlank(text(a, "edital"), provider.getBaseUrl()));
			out.add(r);
		}
		return out;
	}

	/** Lotes de veiculo (com detalhe) de todos os leiloes; achatados para o feed paginar em memoria. */
	public List<Map<String, Object>> fetchVehicleLots(AuctionProvider provider) {
		List<Map<String, Object>> lots = new ArrayList<>();
		JsonNode aucs = listarLeiloes(provider);
		if (aucs == null || !aucs.isArray()) {
			return lots;
		}
		for (JsonNode a : aucs) {
			String auctionId = text(a, "id");
			if (auctionId == null) {
				continue;
			}
			JsonNode ids = getJson(provider.getBaseUrl() + "/loteGeral/buscartListaLotePorLeilaoId?idLeilao=" + auctionId);
			if (ids == null || !ids.isArray()) {
				continue;
			}
			for (JsonNode idNode : ids) {
				String loteId = text(idNode, "id");
				if (loteId == null) {
					continue;
				}
				JsonNode lot = getJson(provider.getBaseUrl() + "/loteGeral/buscarLotePorId?idLote=" + loteId);
				if (lot == null || lot.path("veiculos").size() == 0) {
					continue; // sem veiculo = imovel/outro -> ignora
				}
				if (orgaosOnly && !isOrgao(lot)) {
					continue;
				}
				lots.add(toLotMap(auctionId, lot));
			}
		}
		return lots;
	}

	private Map<String, Object> toLotMap(String auctionId, JsonNode lot) {
		Long lance = asLong(lot.get("valorLance"));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("auctionId", auctionId);
		m.put("lotId", text(lot, "loteId"));
		m.put("lotNumber", lot.hasNonNull("descricaoNumero") ? "Lote " + lot.get("descricaoNumero").asText() : null);
		m.put("lotType", text(lot, "status"));
		m.put("vehicleDescription", text(lot, "descricao"));
		m.put("currentBidValue", lance);
		// Sem campo de piso explicito; o lance atual alimenta o piso (o sink mantem o menor ja visto).
		m.put("minimumBidValue", lance);
		m.put("closingDate", brDate(text(lot, "dataEncerramento")));
		m.put("lotStatus", text(lot, "status"));
		return m;
	}

	private boolean isOrgao(JsonNode lot) {
		String comitente = lot.path("comitente").asText("").toLowerCase(Locale.ROOT);
		return ORGAO_KEYWORDS.stream().anyMatch(comitente::contains);
	}

	private JsonNode listarLeiloes(AuctionProvider provider) {
		StringBuilder status = new StringBuilder();
		for (int i = 0; i < STATUSES.size(); i++) {
			status.append(i == 0 ? "\"" : ",\"").append(STATUSES.get(i)).append("\"");
		}
		String body = "{\"status\":[" + status + "],\"maximoRetorno\":200,\"ordenacao\":\"NUMERO\",\"posicao\":0}";
		return postJson(provider.getBaseUrl() + "/leilao/listarLeiloes", body);
	}

	// ---------------------------------- HTTP / parsing ----------------------------------

	private JsonNode getJson(String url) {
		try {
			return MAPPER.readTree(connect(url).method(Connection.Method.GET).execute().body());
		} catch (Exception ex) {
			LOG.warn("MC GET falhou {}: {}", url, ex.getMessage());
			return null;
		}
	}

	private JsonNode postJson(String url, String jsonBody) {
		try {
			return MAPPER.readTree(connect(url)
					.header("Content-Type", "application/json")
					.requestBody(jsonBody)
					.method(Connection.Method.POST)
					.execute()
					.body());
		} catch (Exception ex) {
			LOG.warn("MC POST falhou {}: {}", url, ex.getMessage());
			return null;
		}
	}

	private Connection connect(String url) {
		return Jsoup.connect(url)
				.userAgent(userAgent)
				.header("Accept", "application/json")
				.ignoreContentType(true)
				.maxBodySize(0)
				.timeout(timeoutMs);
	}

	/** ISO com offset (ex.: 2026-02-12T17:00:00.000+0000) -> "dd/MM/yyyy HH:mm" no fuso de SP. */
	private static String brDate(String iso) {
		if (iso == null || iso.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZoneSameInstant(SP).format(BR);
		} catch (RuntimeException ex) {
			try {
				return OffsetDateTime.parse(iso).atZoneSameInstant(SP).format(BR);
			} catch (RuntimeException ex2) {
				return null;
			}
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private static Long asLong(JsonNode v) {
		if (v == null || v.isNull()) {
			return null;
		}
		return new BigDecimal(v.asText()).longValue();
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				return v;
			}
		}
		return null;
	}
}
