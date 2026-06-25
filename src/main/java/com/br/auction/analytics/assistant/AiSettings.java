package com.br.auction.analytics.assistant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configuracao (singleton) da forma de conexao com a IA usada pelo assistente do
 * B.I.: qual provedor, o token/credencial, a URL base, o modelo e o timeout.
 * Editavel em runtime na tela BI / IA -> Configuracoes de IA.
 */
@Schema(name = "AiSettings", description = "Forma de conexao com a IA do assistente do B.I.")
@Entity
@Table(name = "tbAiSettings")
public class AiSettings {

    /** Identificador fixo do registro unico de configuracao. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Schema(description = "Provedor de IA ativo.")
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 40, nullable = false)
    private AiProvider provider = AiProvider.CLAUDE_CLI;

    @Schema(description = "Token/chave de API (provedores de nuvem). Guardado, nunca retornado.")
    @Column(name = "apiKey", length = 500)
    private String apiKey;

    @Schema(description = "URL base do provedor (Ollama, ou endpoint compativel). Em branco usa o padrao do provedor.")
    @Column(name = "baseUrl", length = 300)
    private String baseUrl;

    @Schema(description = "Modelo a usar. Em branco usa o padrao do provedor.")
    @Column(name = "model", length = 120)
    private String model;

    @Schema(description = "Comando/binario do Claude CLI (provedor local CLAUDE_CLI).")
    @Column(name = "claudeCommand", length = 200)
    private String claudeCommand;

    @Schema(description = "Tempo maximo de espera por resposta, em segundos.")
    @Column(name = "timeoutSeconds", nullable = false)
    private int timeoutSeconds = 120;

    @Schema(description = "Modo bypass: a IA pode consultar qualquer tabela do banco (SOMENTE LEITURA), "
            + "ignorando a whitelist de views. As travas de escrita continuam ativas.")
    @Column(name = "bypassGuard")
    private Boolean bypassGuard = Boolean.FALSE;

    public AiSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AiProvider getProvider() {
        return provider;
    }

    public void setProvider(AiProvider provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getClaudeCommand() {
        return claudeCommand;
    }

    public void setClaudeCommand(String claudeCommand) {
        this.claudeCommand = claudeCommand;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isBypassGuard() {
        return Boolean.TRUE.equals(bypassGuard);
    }

    public void setBypassGuard(boolean bypassGuard) {
        this.bypassGuard = bypassGuard;
    }

    /** Indica se ha token/chave de API definido (sem expor o valor). */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
