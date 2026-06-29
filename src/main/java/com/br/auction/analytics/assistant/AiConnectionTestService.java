package com.br.auction.analytics.assistant;

import org.springframework.stereotype.Service;

/**
 * Testa a conexão com o provedor de IA sem persistir. Orquestra a configuração efetiva
 * ({@link AiSettingsService}) e o roteador de provedores ({@link AiCompletionRouter}). Fica num
 * serviço próprio (e não no controller nem no {@code AiSettingsService}) para manter o controller
 * fino sem criar ciclo de dependência com o roteador.
 */
@Service
public class AiConnectionTestService {

	private final AiSettingsService settingsService;
	private final AiCompletionRouter router;

	public AiConnectionTestService(AiSettingsService settingsService, AiCompletionRouter router) {
		this.settingsService = settingsService;
		this.router = router;
	}

	public AiSettingsTestResultResponse test(AiSettingsRequest request) {
		AiSettings effective = settingsService.mergeForTest(request);
		try {
			if (!router.isAvailableWith(effective)) {
				return new AiSettingsTestResultResponse(false, "Provedor indisponivel ou credenciais invalidas.", null);
			}
			String sample = router.completeWith(effective,
					"Responda em uma unica linha curta, sem markdown.",
					"Diga apenas: conexao ok.");
			return new AiSettingsTestResultResponse(true, "Conexao estabelecida com sucesso.", sample);
		} catch (AiUnavailableException ex) {
			return new AiSettingsTestResultResponse(false, ex.getMessage(), null);
		} catch (RuntimeException ex) {
			return new AiSettingsTestResultResponse(false, "Falha ao testar: " + ex.getMessage(), null);
		}
	}
}
