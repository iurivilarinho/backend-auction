package com.br.auction.analytics.nlquery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.br.auction.analytics.assistant.AiCompletionPort;
import com.br.auction.analytics.assistant.AiSettingsService;
import com.br.auction.analytics.assistant.AiUnavailableException;
import com.br.auction.analytics.nlquery.NlQueryDtos.ChartSpec;
import com.br.auction.analytics.nlquery.NlQueryDtos.ColumnMeta;
import com.br.auction.analytics.nlquery.NlQueryDtos.NlQueryResponse;
import com.br.auction.analytics.nlquery.NlQueryDtos.QueryResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

/**
 * Assistente <b>text-to-HQL</b>: a IA gera HQL (Hibernate Query Language) sobre o
 * modelo JPA inteiro (todas as entidades reais, sem allowlist nem view de banco);
 * o Hibernate traduz para o dialeto do SGBD — portavel, sem SQL nativo. O schema
 * apresentado a IA e montado do <b>metamodelo JPA</b> em runtime. Sao ocultadas
 * apenas as views auxiliares {@code Bi*} e as entidades sensiveis (segredos). A
 * blindagem garante somente leitura ({@link HqlGuard}).
 */
@Service
public class HqlQueryService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HqlQueryService.class);
    private static final int MAX_ROWS = 1000;
    /**
     * Entidades NAO expostas a IA. Sao ocultadas: as views auxiliares {@code Bi*}; as que
     * guardam segredos (chaves de IA, credenciais); o encanamento de integracao e os blobs
     * (imagens/documentos) — esses ultimos nao tem valor analitico e so incham o prompt
     * (cada token a mais conta no limite diario do provedor de IA).
     */
    private static final Set<String> EXCLUDED_ENTITIES = Set.of(
            "BiVeiculo", "BiAquisicao", "BiGasto", "BiSavedView", "AiSettings", "Credential",
            "AuctionItemImage", "AcquisitionDocument", "ExpenseQuote",
            "Integration", "IntegrationRun", "IntegrationItemLog", "IntegrationSource",
            "FieldMapping", "SourceModel", "SourceModelField");

    private final AiCompletionPort ai;
    private final ObjectMapper objectMapper;
    private final AiSettingsService aiSettingsService;
    private final HqlGuard guard;

    @PersistenceContext
    private EntityManager entityManager;

    public HqlQueryService(AiCompletionPort ai, ObjectMapper objectMapper, AiSettingsService aiSettingsService,
            HqlGuard guard) {
        this.ai = ai;
        this.objectMapper = objectMapper;
        this.aiSettingsService = aiSettingsService;
        this.guard = guard;
    }

    public boolean aiAvailable() {
        return ai.isAvailable();
    }

    public NlQueryResponse ask(String question, String previousHql) {
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma pergunta.");
        }
        String q = question.trim();
        String schema = schemaText();
        Set<String> allowed = allowedEntityNames();

        Plan plan;
        try {
            plan = parse(ai.complete(systemPrompt(schema), userPrompt(q, previousHql)));
        } catch (AiUnavailableException offline) {
            return messageOnly(q, "Assistente de IA indisponivel.", true);
        }
        if (plan == null || plan.hql == null || plan.hql.isBlank()) {
            String msg = plan != null && plan.message != null ? plan.message
                    : "Nao consegui montar uma consulta para isso. Tente reformular (marca, modelo, ano, cidade, "
                            + "lance, FIPE, leilao, aquisicao, lucro...).";
            return messageOnly(q, msg, false);
        }
        Outcome outcome = executeWithRetry(q, plan, schema, allowed);
        if (outcome == null) {
            return messageOnly(q, "Tentei montar essa consulta mas nao consegui executa-la. Pode reformular?", false);
        }
        String message = outcome.result.rows().isEmpty()
                ? "Nao encontrei nenhum resultado para esse pedido."
                : friendly(outcome.plan.message, "Aqui esta o que encontrei.");
        return new NlQueryResponse(q, outcome.hql, outcome.plan.explanation, outcome.plan.chart,
                outcome.result.columns(), outcome.result.rows(), outcome.result.rows().size(), true, false, message,
                outcome.plan.limit);
    }

    /** Executa o HQL; se falhar, pede UMA correcao a IA. Devolve null se nao der. */
    private Outcome executeWithRetry(String question, Plan plan, String schema, Set<String> allowed) {
        try {
            String safe = guard.sanitize(plan.hql, allowed);
            return new Outcome(safe, execute(safe, plan.limit), plan);
        } catch (RuntimeException first) {
            LOG.warn("HQL gerado falhou (tentativa 1): {} -- HQL: {}", describe(first), plan.hql);
            try {
                Plan fixed = parse(ai.complete(systemPrompt(schema), "O pedido foi: " + question
                        + "\nO HQL abaixo falhou: " + describe(first) + "\nHQL:\n" + plan.hql
                        + "\nCorrija e devolva o JSON. Se nao der, devolva \"hql\": null e uma \"message\"."));
                if (fixed == null || fixed.hql == null || fixed.hql.isBlank()) {
                    return null;
                }
                String safe = guard.sanitize(fixed.hql, allowed);
                return new Outcome(safe, execute(safe, fixed.limit), fixed);
            } catch (RuntimeException second) {
                LOG.warn("HQL corrigido tambem falhou (tentativa 2): {}", describe(second));
                return null;
            }
        }
    }

    public NlQueryResponse run(String hql, ChartSpec chart, String title, Integer limit) {
        String safe = guard.sanitize(hql, allowedEntityNames());
        QueryResult result;
        try {
            result = execute(safe, limit);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nao foi possivel recarregar esta visao (a consulta salva falhou).");
        }
        ChartSpec safeChart = chart != null ? chart : new ChartSpec("none", null, List.of(), null);
        String name = title != null && !title.isBlank() ? title : "Visao salva";
        return new NlQueryResponse(name, safe, "", safeChart, result.columns(), result.rows(),
                result.rows().size(), false, false, "Visao recarregada.", limit);
    }

    public byte[] exportExcel(String hql) {
        String safe = guard.sanitize(hql, allowedEntityNames());
        QueryResult result = runOrBadRequest(safe);
        return buildExcel(result);
    }

    // ------------------------------------------------------------- execucao

    public QueryResult execute(String hql, Integer limit) {
        int max = limit != null && limit > 0 ? Math.min(limit, MAX_ROWS) : MAX_ROWS;
        List<Tuple> tuples = entityManager.createQuery(hql, Tuple.class).setMaxResults(max).getResultList();
        List<ColumnMeta> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        boolean first = true;
        for (Tuple tuple : tuples) {
            List<TupleElement<?>> elements = tuple.getElements();
            if (first) {
                for (int i = 0; i < elements.size(); i++) {
                    TupleElement<?> el = elements.get(i);
                    String name = el.getAlias() != null ? el.getAlias() : "col" + (i + 1);
                    columns.add(new ColumnMeta(name, simpleType(el.getJavaType())));
                }
                first = false;
            }
            List<Object> row = new ArrayList<>(elements.size());
            for (int i = 0; i < elements.size(); i++) {
                row.add(tuple.get(i));
            }
            rows.add(row);
        }
        return new QueryResult(columns, rows);
    }

    private QueryResult runOrBadRequest(String hql) {
        try {
            return execute(hql, null);
        } catch (HqlGuardException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A consulta gerada nao pode ser executada. Tente reformular a pergunta.");
        }
    }

    // ------------------------------------------------------------- schema (metamodelo)

    /** Todas as entidades mapeadas, menos as ocultas (views Bi* e segredos). */
    private Set<String> allowedEntityNames() {
        Set<String> names = new LinkedHashSet<>();
        for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
            if (!EXCLUDED_ENTITIES.contains(entity.getName())) {
                names.add(entity.getName());
            }
        }
        return names;
    }

    /** Monta o schema (entidades + atributos) a partir do metamodelo JPA, expondo o banco inteiro. */
    private String schemaText() {
        StringBuilder sb = new StringBuilder(
                "Modelo JPA de leiloes de veiculos. Entidades (use SO estas), com seus atributos:\n\n");
        for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
            if (EXCLUDED_ENTITIES.contains(entity.getName())) {
                continue;
            }
            sb.append(entity.getName()).append('(');
            boolean firstAttr = true;
            for (Attribute<?, ?> attr : entity.getAttributes()) {
                if (!firstAttr) {
                    sb.append(", ");
                }
                // So o nome do atributo (sem o tipo Java) — economiza tokens do provedor de IA.
                sb.append(attr.getName());
                firstAttr = false;
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------- prompt / parse

    private String systemPrompt(String schema) {
        return schema + """

                Gere UM unico SELECT HQL (somente leitura) sobre as entidades acima, juntando-as pelas associacoes.
                Regras:
                - Projecao explicita com alias em CADA coluna. Nunca SELECT *, UPDATE/DELETE/INSERT nem ';'.
                - "top N": ordene e ponha "limit": N no JSON (HQL nao tem LIMIT inline).
                - ORDER BY/WHERE nao aceitam alias do SELECT dentro de expressao (erro "could not interpret path"):
                  use o alias sozinho (ex.: ORDER BY economia DESC NULLS LAST) OU repita a expressao inteira.
                - Texto case-insensitive com upper()/lower(). Dados: marca/modelo em MAIUSCULAS, cidade em minusculas.
                - NAO use radians()/pi() (use a constante 0.017453292519943295 para pi/180).
                - distance_km(...), media_finalizada_modelo(...) e media_finalizada_regiao(...) sao FUNCOES:
                  chame-as no SELECT/WHERE/ORDER BY. NUNCA as coloque no FROM nem as trate como entidade.
                  Nunca escreva LIMIT no HQL (use o campo "limit" do JSON).

                Glossario:
                - AuctionItem i = veiculo/lote: brand (marca), model (modelo), vehicleYear (ano), vehicleDescription,
                  lotType (condicao: CONSERVADO/SUCATA/MONTA; "conservado" = upper(i.lotType) LIKE '%CONSERV%'),
                  currentBidValue (lance atual), fipeValue. Leilao = i.auction (Auction a): city, stateCode (UF),
                  status, closingDate, auctioneer, providerCode.
                - Status: 'Finalizado' = encerrado; 'Publicado'/'Em Andamento' = aberto. "comprar agora" -> a.status <> 'Finalizado'.
                - Acquisition aq (arrematado): aq.auctionItem, acquisitionValue, saleValue, status, acquiredAt, soldAt.
                  Gasto = AcquisitionExpense e (e.acquisition, type, value). Lucro = aq.saleValue - aq.acquisitionValue
                  - (select coalesce(sum(e.value),0) from AcquisitionExpense e where e.acquisition = aq).
                - MEDIA HISTORICA ("quanto o modelo costuma ser arrematado" / referencia de mercado): use a FUNCAO
                  media_finalizada_modelo(i.model) (ou media_finalizada_regiao(i.model, a.stateCode) p/ "pela regiao").
                  NAO calcule a media na mao (agrupar por ano/marca infla). economia = media_finalizada_modelo(i.model) - i.currentBidValue.
                - DISTANCIA em km: FUNCAO distance_km(og.latitude, og.longitude, g.latitude, g.longitude). Coordenadas
                  SO existem em CityGeocode. Para perguntas com distancia/km/raio/"ponto de partida", copie este modelo
                  e mude apenas os filtros:
                    SELECT i.brand AS marca, i.model AS modelo, i.currentBidValue AS lance, a.city AS cidade,
                           media_finalizada_modelo(i.model) AS media,
                           (media_finalizada_modelo(i.model) - i.currentBidValue) AS economia,
                           distance_km(og.latitude, og.longitude, g.latitude, g.longitude) AS distancia_km
                    FROM AuctionItem i, Auction a, CityGeocode g, DistanceSetting ds, CityGeocode og
                    WHERE a = i.auction AND lower(g.city)=lower(a.city) AND upper(g.state)=upper(a.stateCode) AND g.resolved=true
                      AND ds.id=1 AND lower(og.city)=lower(ds.originCity) AND upper(og.state)=upper(ds.originState) AND og.resolved=true
                      AND a.status <> 'Finalizado' AND distance_km(og.latitude, og.longitude, g.latitude, g.longitude) < 400
                    ORDER BY economia DESC NULLS LAST
                - Nao ha campo moto/carro; se preciso, use LIKE por modelos conhecidos.

                Grafico: "composed" = 1a serie barra, demais linha; "scatter" = x e 1a serie numericas.
                Responda SO um JSON: {"hql":"<SELECT ou null>","limit":<N ou omita>,
                "chart":{"type":"bar|line|area|scatter|composed|pie|none","x":"<col>","series":["<col>"],"title":"<t>"},
                "explanation":"<tecnico>","message":"<resposta curta>"}
                """;
    }

    private String userPrompt(String question, String previousHql) {
        StringBuilder sb = new StringBuilder();
        if (previousHql != null && !previousHql.isBlank()) {
            sb.append("Contexto — consulta anterior (use como base se for refinamento; senao ignore):\n")
                    .append(previousHql).append("\n\n");
        }
        return sb.append("Pedido: ").append(question).toString();
    }

    private Plan parse(String raw) {
        int start = raw == null ? -1 : raw.indexOf('{');
        int end = raw == null ? -1 : raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));
            String hql = text(node, "hql");
            String message = text(node, "message");
            if (hql == null && message == null) {
                return null;
            }
            Plan plan = new Plan();
            plan.hql = hql;
            plan.explanation = text(node, "explanation") != null ? text(node, "explanation") : "";
            plan.message = message;
            JsonNode limit = node.get("limit");
            plan.limit = limit != null && limit.canConvertToInt() ? limit.asInt() : null;
            JsonNode chartNode = node.get("chart");
            if (hql == null || chartNode == null) {
                plan.chart = new ChartSpec("none", null, List.of(), null);
            } else {
                plan.chart = new ChartSpec(textOr(chartNode, "type", "bar"), text(chartNode, "x"), series(chartNode),
                        text(chartNode, "title"));
            }
            return plan;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> series(JsonNode chartNode) {
        JsonNode s = chartNode.get("series");
        if (s == null || !s.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        s.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                out.add(item.asText().trim());
            }
        });
        return out;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || !v.isTextual() || v.asText().isBlank() ? null : v.asText().trim();
    }

    private String textOr(JsonNode node, String field, String fallback) {
        String v = text(node, field);
        return v != null ? v : fallback;
    }

    // ------------------------------------------------------------- helpers

    private NlQueryResponse messageOnly(String question, String message, boolean aiOffline) {
        return new NlQueryResponse(question, null, "", new ChartSpec("none", null, List.of(), null),
                List.of(), List.of(), 0, false, aiOffline, message, null);
    }

    private String friendly(String aiMessage, String fallback) {
        return aiMessage != null && !aiMessage.isBlank() ? aiMessage : fallback;
    }

    private String describe(RuntimeException e) {
        String m = e.getMessage();
        if (m == null) {
            return e.getClass().getSimpleName();
        }
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    private String simpleType(Class<?> type) {
        if (type == null) {
            return "text";
        }
        if (Number.class.isAssignableFrom(type)) {
            return "number";
        }
        if (Boolean.class.isAssignableFrom(type)) {
            return "boolean";
        }
        if (Temporal.class.isAssignableFrom(type) || Date.class.isAssignableFrom(type)) {
            return "date";
        }
        return "text";
    }

    private byte[] buildExcel(QueryResult result) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Consulta");
            List<ColumnMeta> columns = result.columns();
            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                header.createCell(c).setCellValue(columns.get(c).name());
            }
            List<List<Object>> rows = result.rows();
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object value = values.get(c);
                    if (value == null) {
                        cell.setBlank();
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else if (value instanceof Boolean bool) {
                        cell.setCellValue(bool);
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o Excel da consulta.", e);
        }
    }

    private static final class Plan {
        private String hql;
        private String explanation;
        private String message;
        private Integer limit;
        private ChartSpec chart;
    }

    private record Outcome(String hql, QueryResult result, Plan plan) {
    }
}
