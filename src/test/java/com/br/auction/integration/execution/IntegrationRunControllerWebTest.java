package com.br.auction.integration.execution;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testa a camada web do {@link IntegrationRunController}: o resumo por status é um DTO tipado cujas
 * chaves JSON continuam em maiúsculas (nomes de RunStatus), preservando o contrato do frontend.
 */
@WebMvcTest(IntegrationRunController.class)
class IntegrationRunControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IntegrationRunService service;

	@Test
	void runSummaryReturnsTypedResponseWithStatusKeys() throws Exception {
		when(service.runSummary()).thenReturn(new IntegrationRunSummaryResponse(1, 2, 3, 4, 5));

		mockMvc.perform(get("/api/integration/runs/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.RUNNING").value(1))
				.andExpect(jsonPath("$.SUCCESS").value(2))
				.andExpect(jsonPath("$.PARTIAL").value(3))
				.andExpect(jsonPath("$.FAILED").value(4))
				.andExpect(jsonPath("$.CANCELLED").value(5));
	}
}
