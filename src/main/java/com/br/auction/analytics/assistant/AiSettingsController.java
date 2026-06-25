package com.br.auction.analytics.assistant;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.analytics.assistant.AiSettingsDtos.AiSettingsRequest;
import com.br.auction.analytics.assistant.AiSettingsDtos.AiSettingsView;
import com.br.auction.analytics.assistant.AiSettingsDtos.ProviderOption;
import com.br.auction.analytics.assistant.AiSettingsDtos.TestResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Configuracao da integracao com IA do assistente de B.I. */
@RestController
@RequestMapping("/api/bi/ai-settings")
@Tag(name = "BI - Configuracoes de IA", description = "Forma de conexao com a IA do assistente do B.I.")
public class AiSettingsController {

    private final AiSettingsService settingsService;
    private final AiCompletionRouter router;

    public AiSettingsController(AiSettingsService settingsService, AiCompletionRouter router) {
        this.settingsService = settingsService;
        this.router = router;
    }

    @GetMapping
    @Operation(summary = "Configuracao atual da integracao com IA (sem expor o token).")
    @ApiResponse(responseCode = "200", description = "Configuracao efetiva atual.")
    public AiSettingsView current() {
        return AiSettingsView.of(settingsService.global());
    }

    @GetMapping("/providers")
    @Operation(summary = "Lista os provedores de IA disponiveis para selecao.")
    public List<ProviderOption> providers() {
        return ProviderOption.all();
    }

    @PutMapping
    @Operation(summary = "Salva a configuracao da integracao com IA.")
    @ApiResponse(responseCode = "200", description = "Configuracao salva.")
    public AiSettingsView save(@RequestBody AiSettingsRequest request) {
        return AiSettingsView.of(settingsService.saveGlobal(request.toForm()));
    }

    @PostMapping("/test")
    @Operation(summary = "Testa a conexao com o provedor informado (sem persistir).")
    @ApiResponse(responseCode = "200", description = "Resultado do teste.")
    public TestResult test(@RequestBody AiSettingsRequest request) {
        AiSettings effective = settingsService.mergeForTest(request.toForm());
        try {
            if (!router.isAvailableWith(effective)) {
                return new TestResult(false, "Provedor indisponivel ou credenciais invalidas.", null);
            }
            String sample = router.completeWith(effective,
                    "Responda em uma unica linha curta, sem markdown.",
                    "Diga apenas: conexao ok.");
            return new TestResult(true, "Conexao estabelecida com sucesso.", sample);
        } catch (AiUnavailableException e) {
            return new TestResult(false, e.getMessage(), null);
        } catch (RuntimeException e) {
            return new TestResult(false, "Falha ao testar: " + e.getMessage(), null);
        }
    }
}
