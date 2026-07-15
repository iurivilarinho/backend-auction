package com.br.auction.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrDateTimeTest {

	@Test
	void parseiaInstantUtcComZ() {
		// 2026-06-29T11:30:00Z -> 08:30 no fuso de SP (UTC-3)
		assertThat(BrDateTime.toBr("2026-06-29T11:30:00.000Z")).isEqualTo("29/06/2026 08:30");
	}

	@Test
	void parseiaIsoComOffsetNumericoSemDoisPontos() {
		// formato do MC (…+0000)
		assertThat(BrDateTime.toBr("2026-02-12T17:00:00.000+0000")).isEqualTo("12/02/2026 14:00");
	}

	@Test
	void parseiaIsoComOffsetLocalDeSp() {
		// ja no horario local de SP (-03:00): mantem 17:00
		assertThat(BrDateTime.toBr("2026-02-12T17:00:00-03:00")).isEqualTo("12/02/2026 17:00");
	}

	@Test
	void parseiaIsoSemZonaComoHorarioDeSp() {
		// este e o caso que o parser antigo (Instant.parse) devolvia null -> inicio/encerramento sumia
		assertThat(BrDateTime.toBr("2026-06-29T11:30:00")).isEqualTo("29/06/2026 11:30");
	}

	@Test
	void parseiaSomenteData() {
		assertThat(BrDateTime.toBr("2026-06-29")).isEqualTo("29/06/2026 00:00");
	}

	@Test
	void parseiaFormatoBrasileiro() {
		assertThat(BrDateTime.toBr("29/06/2026 11:30")).isEqualTo("29/06/2026 11:30");
		assertThat(BrDateTime.toBr("29/06/2026")).isEqualTo("29/06/2026 00:00");
	}

	@Test
	void devolveNullParaVazioOuInvalido() {
		assertThat(BrDateTime.toBr(null)).isNull();
		assertThat(BrDateTime.toBr("   ")).isNull();
		assertThat(BrDateTime.toBr("nao-e-data")).isNull();
	}
}
