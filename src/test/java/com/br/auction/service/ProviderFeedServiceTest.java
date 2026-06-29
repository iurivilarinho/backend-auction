package com.br.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.response.LotLiveFeedPageResponse;
import com.br.auction.service.AuctionDetranService.LotLiveData;

/**
 * Regressão do feed ao-vivo: deve consultar apenas os itens do provedor pedido (via repositório),
 * para não vazar itens de outro provedor (ex.: LEILO) no feed ao-vivo do DETRAN — o que fazia o sink
 * falhar com "Leilao pai nao encontrado".
 */
@ExtendWith(MockitoExtension.class)
class ProviderFeedServiceTest {

	@Mock
	private AuctionDetranService detranService;

	@Mock
	private LeiloService leiloService;

	@Mock
	private McLeilaoService mcLeilaoService;

	@Mock
	private AuctionItemRepository auctionItemRepository;

	private ProviderFeedService service;

	@BeforeEach
	void setUp() {
		// throttle 0 para nao dormir no teste
		service = new ProviderFeedService(detranService, leiloService, mcLeilaoService, auctionItemRepository, 3, 300, 0L);
	}

	@Test
	void lotsLiveConsultaApenasOProvedorPedido() {
		Auction auction = new Auction();
		auction.setProviderCode("DETRAN_MG");
		auction.setDetranAuctionId("AUC-1");
		AuctionItem item = new AuctionItem();
		item.setLotId("L1");
		item.setAuction(auction);

		when(auctionItemRepository.findByAuctionProviderCode("DETRAN_MG")).thenReturn(List.of(item));
		when(detranService.fetchLotLive(any(AuctionProvider.class), eq("L1")))
				.thenReturn(Optional.of(new LotLiveData(new BigDecimal("100.00"), null, "1")));

		LotLiveFeedPageResponse page = service.lotsLive("DETRAN_MG", 1, 50);

		verify(auctionItemRepository).findByAuctionProviderCode("DETRAN_MG");
		verify(auctionItemRepository, never()).findAll();
		assertThat(page.getLots()).hasSize(1);
		assertThat(page.getLots().get(0).getAuctionId()).isEqualTo("AUC-1");
		assertThat(page.getLots().get(0).getCurrentBidValue()).isEqualTo("100.00");
	}
}
