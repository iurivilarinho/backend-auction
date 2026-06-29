package com.br.auction.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Normalização de cidade/estado para geocodificação: provedores (ex.: LEILO) gravam a UF grudada no
 * nome ("FORTALEZA - CE") e um stateCode genérico ("GO" para todas). O sufixo deve virar o estado
 * real e sair do nome, senão o Nominatim não acha a cidade.
 */
class GeocodingServiceTest {

	@Test
	void usaUfRealDoSufixoEremoveDoNome() {
		assertThat(GeocodingService.cleanCityState("GOIÂNIA - GO", "GO")).containsExactly("GOIÂNIA", "GO");
		assertThat(GeocodingService.cleanCityState("FORTALEZA - CE", "GO")).containsExactly("FORTALEZA", "CE");
		assertThat(GeocodingService.cleanCityState("BELÉM - PA", "GO")).containsExactly("BELÉM", "PA");
		assertThat(GeocodingService.cleanCityState("Fortaleza/CE", "GO")).containsExactly("Fortaleza", "CE");
	}

	@Test
	void mantemCidadesSemSufixo() {
		assertThat(GeocodingService.cleanCityState("Uberlandia", "MG")).containsExactly("Uberlandia", "MG");
		assertThat(GeocodingService.cleanCityState("Belo Horizonte", "MG")).containsExactly("Belo Horizonte", "MG");
	}

	@Test
	void naoConfundeHifenDoNomeComSufixoDeUf() {
		// sufixo de 3+ letras (nao UF) e UF invalida nao sao tratados como estado
		assertThat(GeocodingService.cleanCityState("Sao Joao del-Rei", "MG")).containsExactly("Sao Joao del-Rei", "MG");
		assertThat(GeocodingService.cleanCityState("Cidade - XX", "MG")).containsExactly("Cidade - XX", "MG");
	}
}
