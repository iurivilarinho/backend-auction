package com.br.auction.garage.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.br.auction.garage.models.VehicleAlert;
import com.br.auction.garage.repository.AlertNotificationRepository;
import com.br.auction.garage.repository.VehicleAlertRepository;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.notification.NotificationService;
import com.br.auction.notification.WhatsappNotifier;
import com.br.auction.service.DistanceService;

/**
 * Cobre o motor multi-gatilho via {@code findMatches}: cada gatilho so encontra o lote quando esta
 * habilitado E sua condicao esta satisfeita. Sem raio (radiusKm nulo) para isolar a logica do gatilho.
 */
@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

	@Mock
	private VehicleAlertRepository alertRepository;
	@Mock
	private VehicleAlertService alertService;
	@Mock
	private AlertNotificationRepository notificationRepository;
	@Mock
	private DistanceService distanceService;
	@Mock
	private WhatsappNotifier notifier;
	@Mock
	private NotificationService notificationService;

	private AlertEvaluator evaluator;

	@BeforeEach
	void setUp() {
		evaluator = new AlertEvaluator(alertRepository, alertService, notificationRepository, distanceService,
				notifier, notificationService, 10, 0L);
	}

	@Test
	void newMatchEncontraQualquerCandidato() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyNewMatch(true);
		AuctionItem item = item("Publicado", null, null, null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(item));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(item);
	}

	@Test
	void semNenhumGatilhoNaoEncontraNada() {
		VehicleAlert alert = baseAlert();
		AuctionItem item = item("Em Andamento", BigDecimal.valueOf(1000), null, null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(item));

		assertThat(evaluator.findMatches(alert, 10)).isEmpty();
	}

	@Test
	void openedDisparaApenasEmAndamento() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyOnStart(true);
		AuctionItem emAndamento = item("Em Andamento", null, null, null);
		AuctionItem publicado = item("Publicado", null, null, null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(emAndamento, publicado));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(emAndamento);
	}

	@Test
	void priceAboveDisparaQuandoLancePassaDoTeto() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyPriceAbove(true);
		alert.setThresholdValue(BigDecimal.valueOf(20000));
		AuctionItem acima = item("Em Andamento", BigDecimal.valueOf(25000), null, null);
		AuctionItem abaixo = item("Em Andamento", BigDecimal.valueOf(15000), null, null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(acima, abaixo));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(acima);
	}

	@Test
	void fipeDealDisparaQuandoLanceDentroDoPercentual() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyFipeDeal(true);
		alert.setFipePercent(80);
		AuctionItem barganha = item("Em Andamento", BigDecimal.valueOf(20000), BigDecimal.valueOf(30000), null);
		AuctionItem caro = item("Em Andamento", BigDecimal.valueOf(29000), BigDecimal.valueOf(30000), null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(barganha, caro));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(barganha);
	}

	@Test
	void closingSoonDisparaDentroDaAntecedencia() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyClosingSoon(true);
		alert.setLeadTimeMinutes(60);
		LocalDateTime now = LocalDateTime.now();
		AuctionItem perto = item("Em Andamento", null, null, now.plusMinutes(30));
		AuctionItem longe = item("Em Andamento", null, null, now.plusMinutes(120));
		when(alertService.findCandidates(alert)).thenReturn(List.of(perto, longe));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(perto);
	}

	@Test
	void noBidsClosingDisparaPertoDeEncerrarSemLances() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyNoBidsClosing(true);
		alert.setLeadTimeMinutes(60);
		LocalDateTime now = LocalDateTime.now();
		AuctionItem semLances = item("Em Andamento", BigDecimal.valueOf(500), null, now.plusMinutes(30));
		semLances.setMinimumBidValue(BigDecimal.valueOf(500));
		AuctionItem comLances = item("Em Andamento", BigDecimal.valueOf(600), null, now.plusMinutes(30));
		comLances.setMinimumBidValue(BigDecimal.valueOf(500));
		AuctionItem longe = item("Em Andamento", BigDecimal.valueOf(500), null, now.plusMinutes(120));
		longe.setMinimumBidValue(BigDecimal.valueOf(500));
		when(alertService.findCandidates(alert)).thenReturn(List.of(semLances, comLances, longe));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(semLances);
	}

	@Test
	void soldBelowDisparaSoAposEncerrarEDentroDoAlvo() {
		VehicleAlert alert = baseAlert();
		alert.setNotifySoldBelow(true);
		alert.setSoldBelowValue(BigDecimal.valueOf(18000));
		LocalDateTime now = LocalDateTime.now();
		AuctionItem barato = item("Finalizado", BigDecimal.valueOf(17000), null, now.minusHours(1));
		AuctionItem caro = item("Finalizado", BigDecimal.valueOf(19000), null, now.minusHours(1));
		AuctionItem aindaAberto = item("Em Andamento", BigDecimal.valueOf(17000), null, now.plusHours(1));
		when(alertService.findCandidates(alert)).thenReturn(List.of(barato, caro, aindaAberto));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(barato);
	}

	@Test
	void variosGatilhosDisparaSeQualquerUmBater() {
		VehicleAlert alert = baseAlert();
		alert.setNotifyNewMatch(true);
		alert.setNotifyPriceAbove(true);
		alert.setThresholdValue(BigDecimal.valueOf(20000));
		// Lance baixo nao dispara PRICE_ABOVE, mas NEW_MATCH sim: o lote aparece mesmo assim.
		AuctionItem item = item("Publicado", BigDecimal.valueOf(1000), null, null);
		when(alertService.findCandidates(alert)).thenReturn(List.of(item));

		assertThat(evaluator.findMatches(alert, 10)).containsExactly(item);
	}

	private VehicleAlert baseAlert() {
		VehicleAlert alert = new VehicleAlert();
		alert.setName("teste");
		alert.setNotifyNewMatch(false);
		alert.setNotifyOnStart(false);
		alert.setNotifyPriceAbove(false);
		alert.setNotifyFipeDeal(false);
		alert.setNotifyClosingSoon(false);
		alert.setNotifySoldBelow(false);
		return alert;
	}

	private AuctionItem item(String status, BigDecimal currentBid, BigDecimal fipe, LocalDateTime closing) {
		Auction auction = new Auction();
		auction.setStatus(status);
		auction.setClosingDate(closing);
		AuctionItem item = new AuctionItem();
		item.setAuction(auction);
		item.setCurrentBidValue(currentBid);
		item.setFipeValue(fipe);
		return item;
	}
}
