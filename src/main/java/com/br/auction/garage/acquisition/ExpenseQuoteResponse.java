package com.br.auction.garage.acquisition;

import java.math.BigDecimal;

import com.br.auction.garage.models.ExpenseQuote;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Orcamento de um gasto")
public class ExpenseQuoteResponse {

	private final Long id;
	private final String place;
	private final BigDecimal value;
	private final String url;
	private final String notes;
	private final boolean selected;

	public ExpenseQuoteResponse(ExpenseQuote quote) {
		this.id = quote.getId();
		this.place = quote.getPlace();
		this.value = quote.getValue();
		this.url = quote.getUrl();
		this.notes = quote.getNotes();
		this.selected = Boolean.TRUE.equals(quote.getSelected());
	}

	public String getUrl() {
		return url;
	}

	public Long getId() {
		return id;
	}

	public String getPlace() {
		return place;
	}

	public BigDecimal getValue() {
		return value;
	}

	public String getNotes() {
		return notes;
	}

	public boolean isSelected() {
		return selected;
	}
}
