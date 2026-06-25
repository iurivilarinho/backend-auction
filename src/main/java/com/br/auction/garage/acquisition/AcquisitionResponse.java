package com.br.auction.garage.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.garage.models.Acquisition;
import com.br.auction.response.AuctionItemResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Veiculo adquirido com totais calculados")
public class AcquisitionResponse {

	private final Long id;
	private final AuctionItemResponse vehicle;
	private final AcquisitionStatus status;
	private final String statusLabel;
	private final BigDecimal acquisitionValue;
	private final BigDecimal saleValue;
	private final LocalDate acquiredAt;
	private final LocalDate inspectionDeadline;
	private final LocalDate soldAt;
	private final String notes;
	private final BigDecimal totalExpenses;
	private final BigDecimal totalInvested;
	private final BigDecimal profit;
	private final List<AcquisitionExpenseResponse> expenses;
	private final List<AcquisitionDocumentResponse> documents;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public AcquisitionResponse(Acquisition acquisition) {
		this.id = acquisition.getId();
		this.vehicle = acquisition.getAuctionItem() == null ? null : new AuctionItemResponse(acquisition.getAuctionItem());
		this.status = acquisition.getStatus();
		this.statusLabel = acquisition.getStatus() == null ? null : acquisition.getStatus().getDescription();
		this.acquisitionValue = nullToZero(acquisition.getAcquisitionValue());
		this.saleValue = acquisition.getSaleValue();
		this.acquiredAt = acquisition.getAcquiredAt();
		this.inspectionDeadline = acquisition.getInspectionDeadline();
		this.soldAt = acquisition.getSoldAt();
		this.notes = acquisition.getNotes();
		BigDecimal expensesSum = acquisition.getExpenses().stream()
				.map(expense -> expense.effectiveValue())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		this.totalExpenses = expensesSum;
		this.totalInvested = this.acquisitionValue.add(expensesSum);
		this.profit = acquisition.getSaleValue() == null ? null : acquisition.getSaleValue().subtract(this.totalInvested);
		this.expenses = acquisition.getExpenses().stream().map(AcquisitionExpenseResponse::new).toList();
		this.documents = acquisition.getDocuments().stream()
				.map(document -> new AcquisitionDocumentResponse(document, acquisition.getId()))
				.toList();
		this.createdAt = acquisition.getCreatedAt();
		this.updatedAt = acquisition.getUpdatedAt();
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	public Long getId() {
		return id;
	}

	public AuctionItemResponse getVehicle() {
		return vehicle;
	}

	public AcquisitionStatus getStatus() {
		return status;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public BigDecimal getAcquisitionValue() {
		return acquisitionValue;
	}

	public BigDecimal getSaleValue() {
		return saleValue;
	}

	public LocalDate getAcquiredAt() {
		return acquiredAt;
	}

	public LocalDate getInspectionDeadline() {
		return inspectionDeadline;
	}

	public LocalDate getSoldAt() {
		return soldAt;
	}

	public String getNotes() {
		return notes;
	}

	public BigDecimal getTotalExpenses() {
		return totalExpenses;
	}

	public BigDecimal getTotalInvested() {
		return totalInvested;
	}

	public BigDecimal getProfit() {
		return profit;
	}

	public List<AcquisitionExpenseResponse> getExpenses() {
		return expenses;
	}

	public List<AcquisitionDocumentResponse> getDocuments() {
		return documents;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
