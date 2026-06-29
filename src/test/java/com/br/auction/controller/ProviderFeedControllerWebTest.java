package com.br.auction.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.br.auction.response.LotFeedPageResponse;
import com.br.auction.response.LotFeedResponse;
import com.br.auction.service.ProviderFeedService;

/**
 * Testa a camada web do {@link ProviderFeedController}: o feed devolve um envelope tipado
 * {@code {lots, page, pageSize, hasNext}} (sem Map cru) preservando o contrato do cursor.
 */
@WebMvcTest(ProviderFeedController.class)
class ProviderFeedControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProviderFeedService providerFeedService;

	@Test
	void lotsReturnsTypedCursorPage() throws Exception {
		LotFeedResponse lot = new LotFeedResponse("A1", "L1", "Lote 1", "CONSERVADO", "GOL 1.0", "2015",
				"1000", "900", "01/08/2026 14:00", "ANDAMENTO", List.of("http://img/1.jpg"));
		when(providerFeedService.lots(eq("DETRAN_MG"), eq(null), eq(null), anyInt(), anyInt()))
				.thenReturn(new LotFeedPageResponse(List.of(lot), 1, 200, true));

		mockMvc.perform(get("/api/feed/lots").param("providerCode", "DETRAN_MG"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lots[0].lotId").value("L1"))
				.andExpect(jsonPath("$.lots[0].currentBidValue").value("1000"))
				.andExpect(jsonPath("$.lots[0].imageUrls[0]").value("http://img/1.jpg"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.pageSize").value(200))
				.andExpect(jsonPath("$.hasNext").value(true));
	}
}
