package com.br.auction.garage.acquisition;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Orcamento de um gasto")
public class ExpenseQuoteRequest {

	@NotBlank(message = "O local e obrigatorio")
	@Schema(description = "Local/fornecedor")
	private String place;

	@NotNull(message = "O valor e obrigatorio")
	@Schema(description = "Valor orcado")
	private BigDecimal value;

	@Schema(description = "URL do orcamento (link do produto/loja)")
	private String url;

	@Schema(description = "Observacoes")
	private String notes;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
