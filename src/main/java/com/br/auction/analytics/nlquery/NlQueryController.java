package com.br.auction.analytics.nlquery;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Assistente de B.I. em linguagem natural (text-to-HQL) sobre os dados de leiloes. */
@RestController
@RequestMapping("/api/bi/nl-query")
@Tag(name = "BI - Assistente IA", description = "Consultas em linguagem natural sobre leiloes, veiculos e garagem.")
public class NlQueryController {

    private final HqlQueryService service;

    public NlQueryController(HqlQueryService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @Operation(summary = "Indica se o provedor de IA esta acessivel agora.")
    public ResponseEntity<NlQueryHealthResponse> health() {
        return ResponseEntity.ok(new NlQueryHealthResponse(service.aiAvailable()));
    }

    @PostMapping("/ask")
    @Operation(summary = "Faz uma pergunta em linguagem natural e devolve dados + narrativa.")
    @ApiResponse(responseCode = "200", description = "Resultado da consulta assistida por IA.")
    public ResponseEntity<NlQueryResponse> ask(@RequestBody NlQueryRequest request) {
        return ResponseEntity.ok(service.ask(request.question(), request.previousSql()));
    }

    @PostMapping("/run")
    @Operation(summary = "Reexecuta um HQL ja gerado (revalidado) para recarregar uma visao.")
    public ResponseEntity<NlQueryResponse> run(@RequestBody RunRequest request) {
        return ResponseEntity.ok(service.run(request.sql(), request.chart(), request.title(), request.limit()));
    }

    @PostMapping("/export")
    @Operation(summary = "Exporta o resultado de um HQL em Excel (.xlsx).")
    public ResponseEntity<byte[]> export(@RequestBody ExportRequest request) {
        byte[] bytes = service.exportExcel(request.sql());
        String fileName = request.fileName() != null && !request.fileName().isBlank()
                ? request.fileName()
                : "consulta-bi.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
