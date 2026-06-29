package com.br.auction.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.response.AuctionListJsonResponse;
import com.br.auction.response.LotFeedPageResponse;
import com.br.auction.response.LotFeedResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adapter do provedor LEILO (Grupo Leilo / plataforma Leilomaster). A API {@code api.leilo.com.br}
 * exige token nos endpoints internos, mas dois sao publicos (papel "Visitantes") e bastam para a
 * coleta — precisa de User-Agent de navegador (o Cloudflare barra UA de bot):
 *
 * <ul>
 *   <li>GET {@code /v1/leiloes/lista-site} — lista de leiloes.</li>
 *   <li>POST {@code /v1/lote/busca-elastic} body {@code {from,size,requisicoesBusca:[{}],listaOrdenacao:[]}} — lotes.</li>
 * </ul>
 *
 * Mapeia para o mesmo formato de feed do DETRAN, consumido pelas integracoes. Filtra para VEICULOS
 * (exclui imoveis/equipamentos) e, com {@code lot.leilo.orgaos-only=true}, restringe a comitentes de
 * orgao publico (DETRAN/PRF/etc.).
 */
@Service
public class LeiloService {

	private static final Logger LOG = LoggerFactory.getLogger(LeiloService.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

	/** Tipos considerados veiculo na Leilo. */
	private static final Set<String> VEHICLE_TYPES = Set.of("carros", "motos", "pesados", "utilitarios", "veiculos");
	/** Palavras que identificam comitente de orgao publico. */
	private static final List<String> ORGAO_KEYWORDS = List.of("detran", "prf", "policia", "polícia", "receita",
			"prefeitura", "municipio", "município", "governo", "estado do", "rodoviaria", "rodoviária", "tribunal",
			"ministerio", "ministério", "exercito", "exército", "marinha", "aeronautica", "aeronáutica");

	private final String userAgent;
	private final int timeoutMs;
	private final boolean orgaosOnly;

	public LeiloService(
			@Value("${lot.leilo.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36}") String userAgent,
			@Value("${lot.leilo.timeout-ms:30000}") int timeoutMs,
			@Value("${lot.leilo.orgaos-only:false}") boolean orgaosOnly) {
		this.userAgent = userAgent;
		this.timeoutMs = timeoutMs;
		this.orgaosOnly = orgaosOnly;
	}

	/** Leiloes (lista-site) mapeados para o formato de resposta do feed. */
	public List<AuctionListJsonResponse> fetchAuctions(AuctionProvider provider) {
		List<AuctionListJsonResponse> out = new ArrayList<>();
		JsonNode arr = getJson(provider.getBaseUrl() + "/v1/leiloes/lista-site");
		if (arr == null || !arr.isArray()) {
			return out;
		}
		for (JsonNode a : arr) {
			AuctionListJsonResponse r = new AuctionListJsonResponse();
			r.setAuctionId(text(a, "uid"));
			r.setAuctionNoticeNumber(text(a, "nome"));
			r.setCity(text(a, "cidade"));
			r.setAuctioneer(a.path("leiloeiro").path("nome").asText(null));
			r.setStatus(text(a, "situacao"));
			r.setClosingDate(brDate(text(a, "data")));
			r.setSourceUrl(firstNonBlank(text(a, "editalUrl"), provider.getBaseUrl()));
			out.add(r);
		}
		return out;
	}

	/** Uma pagina de lotes de veiculo (e de orgao, se a flag estiver ligada). */
	public LotFeedPageResponse fetchLotsPage(AuctionProvider provider, int page, int pageSize) {
		int from = Math.max(0, (page - 1) * pageSize);
		String body = "{\"from\":" + from + ",\"size\":" + pageSize + ",\"requisicoesBusca\":[{}],\"listaOrdenacao\":[]}";
		JsonNode arr = postJson(provider.getBaseUrl() + "/v1/lote/busca-elastic", body);
		List<LotFeedResponse> lots = new ArrayList<>();
		if (arr == null || !arr.isArray()) {
			return new LotFeedPageResponse(lots, page, pageSize, false);
		}
		int rawCount = arr.size();
		for (JsonNode lot : arr) {
			if (!isVehicle(lot) || (orgaosOnly && !isOrgao(lot))) {
				continue;
			}
			lots.add(toLot(lot));
		}
		// Ha mais paginas se a fonte devolveu a pagina cheia (o filtro reduz o resultado, mas a fonte continua).
		return new LotFeedPageResponse(lots, page, pageSize, rawCount >= pageSize);
	}

	private LotFeedResponse toLot(JsonNode lot) {
		JsonNode valor = lot.path("valor");
		JsonNode veiculo = lot.path("veiculo");
		Long minimo = asLong(valor.get("minimo"));
		Long lance = asLong(valor.path("lance").get("valor"));
		// No objeto do lote o leilao vem com "id" (UUID), que corresponde ao "uid" da lista-site.
		String auctionId = lot.path("leilao").path("id").asText(null);
		String lotId = text(lot, "lelId");
		String lotNumber = lot.hasNonNull("numero") ? "Lote " + lot.get("numero").asText() : null;
		// Tipo do lote no padrao do sistema (LotType: CONSERVADO/SUCATA). A "categoria" da Leilo
		// (Carros/Motos) nao e o tipo do sistema; classificamos aqui no adapter. Os veiculos da Leilo
		// sao retomadas de banco/seguradora (carros em uso) = CONSERVADO; SUCATA so quando a descricao
		// indica sinistro/perda total/monta.
		String lotType = classifyCondition(lot);
		String vehicleDescription = firstNonBlank(text(lot, "nome"),
				(veiculo.path("infocarMarca").asText("") + " " + veiculo.path("infocarModelo").asText("")).trim());
		// Ano vem estruturado da fonte (a descricao nem sempre traz ano); prioriza o ano-modelo.
		String vehicleYear = vehicleYear(veiculo);
		Long current = lance != null ? lance : minimo;
		return new LotFeedResponse(auctionId, lotId, lotNumber, lotType, vehicleDescription, vehicleYear,
				toStr(current), toStr(minimo), brDate(text(lot, "dataFim")), text(lot, "situacao"),
				imageUrls(lot.path("fotosUrls")));
	}

	/** Ano do veiculo a partir dos campos estruturados (ano-modelo; cai para ano-fabricacao). */
	private static String vehicleYear(JsonNode veiculo) {
		for (String field : new String[] { "anoModelo", "anoFabricacao" }) {
			JsonNode v = veiculo.get(field);
			if (v != null && !v.isNull()) {
				String year = v.asText().trim();
				if (year.matches("\\d{4}")) {
					return year;
				}
			}
		}
		return null;
	}

	/** Palavras que indicam veiculo de sucata/sinistro (do contrario, conservado). */
	private static final List<String> SUCATA_KEYWORDS = List.of("sucata", "sinistro", "sinistrado", "perda total",
			"monta", "remont", "inserviv", "recuperav", "sem motor", "incendiad");

	/**
	 * Classifica o lote no padrao do sistema: SUCATA quando a descricao indica sinistro/sucata; caso
	 * contrario CONSERVADO (os veiculos da Leilo sao retomadas de banco/seguradora, em uso).
	 */
	private static String classifyCondition(JsonNode lot) {
		String text = (lot.path("nome").asText("") + " " + lot.path("veiculo").path("infocarModelo").asText(""))
				.toLowerCase(Locale.ROOT);
		return SUCATA_KEYWORDS.stream().anyMatch(text::contains) ? "SUCATA" : "CONSERVADO";
	}

	private boolean isVehicle(JsonNode lot) {
		String tipo = lot.path("tipo").asText("").toLowerCase(Locale.ROOT);
		return VEHICLE_TYPES.contains(tipo);
	}

	private boolean isOrgao(JsonNode lot) {
		String comitente = lot.path("comitente").path("nome").asText("").toLowerCase(Locale.ROOT);
		return ORGAO_KEYWORDS.stream().anyMatch(comitente::contains);
	}

	private List<String> imageUrls(JsonNode fotos) {
		List<String> urls = new ArrayList<>();
		if (fotos != null && fotos.isArray()) {
			for (JsonNode f : fotos) {
				String u = f.asText(null);
				if (u != null && !u.isBlank()) {
					urls.add(u);
				}
			}
		}
		return urls;
	}

	// ---------------------------------- HTTP / parsing ----------------------------------

	private JsonNode getJson(String url) {
		try {
			String body = connect(url).method(Connection.Method.GET).execute().body();
			return MAPPER.readTree(body);
		} catch (Exception ex) {
			LOG.warn("Leilo GET falhou {}: {}", url, ex.getMessage());
			return null;
		}
	}

	private JsonNode postJson(String url, String jsonBody) {
		try {
			String body = connect(url)
					.header("Content-Type", "application/json")
					.requestBody(jsonBody)
					.method(Connection.Method.POST)
					.execute()
					.body();
			return MAPPER.readTree(body);
		} catch (Exception ex) {
			LOG.warn("Leilo POST falhou {}: {}", url, ex.getMessage());
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

	/** Converte ISO-8601 em UTC (ex.: 2026-06-29T11:30:00.000Z) para "dd/MM/yyyy HH:mm" no fuso de SP. */
	private static String brDate(String iso) {
		if (iso == null || iso.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(iso).atZone(SP).format(BR);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private static Long asLong(JsonNode v) {
		return v == null || v.isNull() ? null : v.asLong();
	}

	/** Valor monetario como texto (a fonte do feed e declarada como STRING; o sink converte). */
	private static String toStr(Long value) {
		return value == null ? null : Long.toString(value);
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
