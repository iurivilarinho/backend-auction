package com.br.auction.garage.alert;

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

import com.br.auction.response.EnumOptionResponse;

/**
 * Testa a camada web do {@link VehicleAlertController}: os tipos de alerta saem como lista tipada de
 * {@code {value, label}} (sem Map cru), com o mesmo JSON de antes.
 */
@WebMvcTest(VehicleAlertController.class)
class VehicleAlertControllerWebTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VehicleAlertService service;

	@MockitoBean
	private AlertScheduler scheduler;

	@MockitoBean
	private AlertFindingService findingService;

	@Test
	void typesReturnTypedEnumOptionsNotMap() throws Exception {
		when(service.types()).thenReturn(List.of(new EnumOptionResponse("PRICE_DROP", "Queda de preco")));

		mockMvc.perform(get("/api/garage/alerts/types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].value").value("PRICE_DROP"))
				.andExpect(jsonPath("$[0].label").value("Queda de preco"));
	}
}
