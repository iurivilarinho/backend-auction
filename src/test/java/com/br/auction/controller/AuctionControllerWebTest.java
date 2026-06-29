package com.br.auction.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.br.auction.enums.PriceStatGroupBy;
import com.br.auction.filter.AuctionItemFilter;
import com.br.auction.filter.PriceStatFilter;
import com.br.auction.integration.execution.IntegrationScheduler;
import com.br.auction.models.AuctionItem;
import com.br.auction.service.AuctionDetranService;
import com.br.auction.service.AuctionService;

/**
 * Testa a camada web do {@link AuctionController} após a padronização: binding dos objetos de filtro
 * (sem lista de @RequestParam) e respostas tipadas (sem Map).
 */
@WebMvcTest(AuctionController.class)
class AuctionControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuctionService auctionService;

	@MockitoBean
	private AuctionDetranService detranService;

	@MockitoBean
	private IntegrationScheduler integrationScheduler;

	@Test
	void findAllItemsBindsFilterObjectFromQueryParams() throws Exception {
		when(auctionService.findAllItems(any(), any())).thenReturn(new PageImpl<AuctionItem>(List.of()));

		mockMvc.perform(get("/api/auction-items")
				.param("brand", "FORD")
				.param("brand", "FIAT")
				.param("minBid", "1000")
				.param("search", "gol")
				.param("providerCode", "DETRAN_MG")
				.param("stateCode", "MG"))
				.andExpect(status().isOk());

		ArgumentCaptor<AuctionItemFilter> captor = ArgumentCaptor.forClass(AuctionItemFilter.class);
		verify(auctionService).findAllItems(captor.capture(), any());
		AuctionItemFilter filter = captor.getValue();
		assertThat(filter.getBrand()).containsExactly("FORD", "FIAT");
		assertThat(filter.getMinBid()).isEqualByComparingTo("1000");
		assertThat(filter.getSearch()).isEqualTo("gol");
		assertThat(filter.getProviderCode()).containsExactly("DETRAN_MG");
		assertThat(filter.getStateCode()).isEqualTo("MG");
	}

	@Test
	void priceStatsBindsFilterObject() throws Exception {
		when(auctionService.priceStats(any())).thenReturn(List.of());

		mockMvc.perform(get("/api/auction-items/price-stats")
				.param("groupBy", "BRAND")
				.param("brand", "FORD"))
				.andExpect(status().isOk());

		ArgumentCaptor<PriceStatFilter> captor = ArgumentCaptor.forClass(PriceStatFilter.class);
		verify(auctionService).priceStats(captor.capture());
		assertThat(captor.getValue().getGroupBy()).isEqualTo(PriceStatGroupBy.BRAND);
		assertThat(captor.getValue().getBrand()).containsExactly("FORD");
	}

	@Test
	void syncAuctionsReturnsTypedResponseNotMap() throws Exception {
		when(integrationScheduler.triggerNow()).thenReturn(2);

		mockMvc.perform(post("/api/auctions/sync"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.started").value(true))
				.andExpect(jsonPath("$.integrations").value(2))
				.andExpect(jsonPath("$.message").exists());
	}
}
