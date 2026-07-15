package com.br.auction.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Conversor tolerante de datas dos provedores para o formato interno {@code "dd/MM/yyyy HH:mm"} no
 * fuso de Sao Paulo. Cada provedor entrega a data num formato diferente (ISO em UTC com {@code Z},
 * ISO com offset numerico, ISO sem zona, ou ja {@code dd/MM/yyyy}); um parser rigido (ex.:
 * {@code Instant.parse}) devolvia {@code null} para os formatos que nao terminavam em {@code Z},
 * fazendo o inicio/encerramento sumir de leiloes que nao eram do DETRAN-MG.
 */
public final class BrDateTime {

	private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter BR_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter BR_IN = DateTimeFormatter.ofPattern("dd/MM/yyyy[ HH:mm[:ss]]");

	private BrDateTime() {
	}

	/**
	 * Converte a data recebida (varios formatos) para {@code "dd/MM/yyyy HH:mm"} no fuso de SP, ou
	 * {@code null} se nao for possivel interpretar.
	 */
	public static String toBr(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String value = raw.trim();

		// 1. Instant em UTC (ex.: 2026-06-29T11:30:00.000Z).
		try {
			return Instant.parse(value).atZone(SP).format(BR_OUT);
		} catch (RuntimeException ignored) {
			// tenta o proximo formato
		}

		// 2. ISO com offset numerico (ex.: ...+00:00, ...-03:00, ...+0000).
		// ISO_OFFSET_DATE_TIME nao aceita offset sem os dois-pontos (...+0000); normaliza para ...+00:00.
		String withColonOffset = value.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2");
		try {
			return OffsetDateTime.parse(withColonOffset, DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZoneSameInstant(SP)
					.format(BR_OUT);
		} catch (RuntimeException ignored) {
			// tenta o proximo formato
		}
		try {
			return OffsetDateTime.parse(withColonOffset).atZoneSameInstant(SP).format(BR_OUT);
		} catch (RuntimeException ignored) {
			// tenta o proximo formato
		}

		// 3. ISO local, sem zona (ex.: 2026-06-29T11:30:00) -> assume horario de SP.
		try {
			return LocalDateTime.parse(value).atZone(SP).format(BR_OUT);
		} catch (RuntimeException ignored) {
			// tenta o proximo formato
		}

		// 4. Somente data ISO (ex.: 2026-06-29).
		try {
			return LocalDate.parse(value).atStartOfDay(SP).format(BR_OUT);
		} catch (RuntimeException ignored) {
			// tenta o proximo formato
		}

		// 5. Formato brasileiro (ex.: 29/06/2026, 29/06/2026 11:30, 29/06/2026 11:30:00).
		try {
			TemporalAccessor parsed = BR_IN.parseBest(value, LocalDateTime::from, LocalDate::from);
			LocalDateTime dateTime = parsed instanceof LocalDateTime local ? local
					: ((LocalDate) parsed).atStartOfDay();
			return dateTime.format(BR_OUT);
		} catch (RuntimeException ignored) {
			// nao foi possivel interpretar
		}

		return null;
	}
}
