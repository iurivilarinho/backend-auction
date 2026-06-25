package com.br.auction.garage.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.br.auction.garage.enums.AcquisitionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um veiculo adquirido")
public class AcquisitionRequest {

	@Schema(description = "ID do item de leilao (veiculo) - obrigatorio na criacao")
	private Long auctionItemId;

	@Schema(description = "Situacao do veiculo")
	private AcquisitionStatus status;

	@Schema(description = "Valor do arremate")
	private BigDecimal acquisitionValue;

	@Schema(description = "Valor de venda")
	private BigDecimal saleValue;

	@Schema(description = "Data de aquisicao")
	private LocalDate acquiredAt;

	@Schema(description = "Prazo de vistoria")
	private LocalDate inspectionDeadline;

	@Schema(description = "Data da venda")
	private LocalDate soldAt;

	@Schema(description = "Observacoes")
	private String notes;

	public Long getAuctionItemId() {
		return auctionItemId;
	}

	public void setAuctionItemId(Long auctionItemId) {
		this.auctionItemId = auctionItemId;
	}

	public AcquisitionStatus getStatus() {
		return status;
	}

	public void setStatus(AcquisitionStatus status) {
		this.status = status;
	}

	public BigDecimal getAcquisitionValue() {
		return acquisitionValue;
	}

	public void setAcquisitionValue(BigDecimal acquisitionValue) {
		this.acquisitionValue = acquisitionValue;
	}

	public BigDecimal getSaleValue() {
		return saleValue;
	}

	public void setSaleValue(BigDecimal saleValue) {
		this.saleValue = saleValue;
	}

	public LocalDate getAcquiredAt() {
		return acquiredAt;
	}

	public void setAcquiredAt(LocalDate acquiredAt) {
		this.acquiredAt = acquiredAt;
	}

	public LocalDate getInspectionDeadline() {
		return inspectionDeadline;
	}

	public void setInspectionDeadline(LocalDate inspectionDeadline) {
		this.inspectionDeadline = inspectionDeadline;
	}

	public LocalDate getSoldAt() {
		return soldAt;
	}

	public void setSoldAt(LocalDate soldAt) {
		this.soldAt = soldAt;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
