package com.br.auction.garage.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.br.auction.garage.enums.ExpenseStatus;
import com.br.auction.garage.enums.ExpenseType;
import com.br.auction.garage.models.AcquisitionExpense;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gasto do veiculo adquirido")
public class AcquisitionExpenseResponse {

	private final Long id;
	private final ExpenseType type;
	private final String typeLabel;
	private final ExpenseStatus status;
	private final String statusLabel;
	private final String description;
	private final BigDecimal value;
	private final String place;
	private final LocalDate incurredAt;
	private final List<ExpenseQuoteResponse> quotes;

	public AcquisitionExpenseResponse(AcquisitionExpense expense) {
		this.id = expense.getId();
		this.type = expense.getType();
		this.typeLabel = expense.getType() == null ? null : expense.getType().getDescription();
		this.status = expense.getStatus();
		this.statusLabel = expense.getStatus() == null ? null : expense.getStatus().getDescription();
		this.description = expense.getDescription();
		this.value = expense.getValue();
		this.place = expense.getPlace();
		this.incurredAt = expense.getIncurredAt();
		this.quotes = expense.getQuotes().stream().map(ExpenseQuoteResponse::new).toList();
	}

	public Long getId() {
		return id;
	}

	public ExpenseType getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public ExpenseStatus getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getValue() {
		return value;
	}

	public String getPlace() {
		return place;
	}

	public LocalDate getIncurredAt() {
		return incurredAt;
	}

	public List<ExpenseQuoteResponse> getQuotes() {
		return quotes;
	}
}
