package com.br.auction.garage.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.br.auction.garage.enums.ExpenseType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados de um gasto. Se vier com orcamentos, comeca como cotacao; senao, ja entra como comprado.")
public class AcquisitionExpenseRequest {

	@NotNull(message = "O tipo e obrigatorio")
	@Schema(description = "Tipo do gasto")
	private ExpenseType type;

	@Schema(description = "Descricao")
	private String description;

	@Schema(description = "Valor (obrigatorio quando comprado direto, sem orcamentos)")
	private BigDecimal value;

	@Schema(description = "Local onde foi comprado")
	private String place;

	@Schema(description = "Data do gasto")
	private LocalDate incurredAt;

	@Valid
	@Schema(description = "Orcamentos (cotacoes). Quando informados, o gasto comeca como cotacao.")
	private List<ExpenseQuoteRequest> quotes = new ArrayList<>();

	public ExpenseType getType() {
		return type;
	}

	public void setType(ExpenseType type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public LocalDate getIncurredAt() {
		return incurredAt;
	}

	public void setIncurredAt(LocalDate incurredAt) {
		this.incurredAt = incurredAt;
	}

	public List<ExpenseQuoteRequest> getQuotes() {
		return quotes;
	}

	public void setQuotes(List<ExpenseQuoteRequest> quotes) {
		this.quotes = quotes;
	}
}
