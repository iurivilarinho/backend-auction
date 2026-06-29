package com.br.auction.analytics.assistant;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Configuracao da integracao com IA do assistente de B.I. */
@RestController
@RequestMapping("/api/bi/ai-settings")
@Tag(name = "BI - Configuracoes de IA", description = "Forma de conexao com a IA do assistente do B.I.")
public class AiSettingsController {

    private final AiSettingsService settingsService;
    private final AiConnectionTestService connectionTestService;

    public AiSettingsController(AiSettingsService settingsService, AiConnectionTestService connectionTestService) {
        this.settingsService = settingsService;
        this.connectionTestService = connectionTestService;
    }

    @GetMapping
    @Operation(summary = "Configuracao atual da integracao com IA (sem expor o token).")
    @ApiResponse(responseCode = "200", description = "Configuracao efetiva atual.")
    public ResponseEntity<AiSettingsResponse> current() {
        return ResponseEntity.ok(settingsService.currentSettings());
    }

    @GetMapping("/providers")
    @Operation(summary = "Lista os provedores de IA disponiveis para selecao.")
    @ApiResponse(responseCode = "200", description = "Provedores retornados com sucesso.")
    public ResponseEntity<List<AiProviderOptionResponse>> providers() {
        return ResponseEntity.ok(settingsService.providerOptions());
    }

    @PutMapping
    @Operation(summary = "Salva a configuracao da integracao com IA.")
    @ApiResponse(responseCode = "200", description = "Configuracao salva.")
    public ResponseEntity<AiSettingsResponse> save(@RequestBody AiSettingsRequest request) {
        return ResponseEntity.ok(new AiSettingsResponse(settingsService.saveGlobal(request)));
    }

    @PostMapping("/test")
    @Operation(summary = "Testa a conexao com o provedor informado (sem persistir).")
    @ApiResponse(responseCode = "200", description = "Resultado do teste.")
    public ResponseEntity<AiSettingsTestResultResponse> test(@RequestBody AiSettingsRequest request) {
        return ResponseEntity.ok(connectionTestService.test(request));
    }
}
