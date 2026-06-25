package com.br.auction.garage.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.models.AuctionItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbAcquisition")
@Schema(description = "Veiculo adquirido em leilao e gerenciado na garagem")
public class Acquisition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "fk_Id_AuctionItem", foreignKey = @ForeignKey(name = "FK_FROM_TBACQUISITION_FOR_TBAUCTIONITEM"))
	@Schema(description = "Veiculo (lote) adquirido")
	private AuctionItem auctionItem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	@Schema(description = "Situacao atual do veiculo")
	private AcquisitionStatus status = AcquisitionStatus.ARREMATADO;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor pago no arremate")
	private BigDecimal acquisitionValue;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor de venda")
	private BigDecimal saleValue;

	@Column
	@Schema(description = "Data de aquisicao")
	private LocalDate acquiredAt;

	@Column
	@Schema(description = "Prazo de vistoria")
	private LocalDate inspectionDeadline;

	@Column
	@Schema(description = "Data da venda")
	private LocalDate soldAt;

	@Column(length = 2000)
	@Schema(description = "Observacoes")
	private String notes;

	@OneToMany(mappedBy = "acquisition", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("incurredAt ASC")
	private List<AcquisitionExpense> expenses = new ArrayList<>();

	@OneToMany(mappedBy = "acquisition", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("type ASC")
	private List<AcquisitionDocument> documents = new ArrayList<>();

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
		if (this.status == null) {
			this.status = AcquisitionStatus.ARREMATADO;
		}
		if (this.acquiredAt == null) {
			this.acquiredAt = LocalDate.now();
		}
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void addExpense(AcquisitionExpense expense) {
		expense.setAcquisition(this);
		this.expenses.add(expense);
	}

	public void addDocument(AcquisitionDocument document) {
		document.setAcquisition(this);
		this.documents.add(document);
	}

	public Long getId() {
		return id;
	}

	public AuctionItem getAuctionItem() {
		return auctionItem;
	}

	public void setAuctionItem(AuctionItem auctionItem) {
		this.auctionItem = auctionItem;
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

	public List<AcquisitionExpense> getExpenses() {
		return expenses;
	}

	public List<AcquisitionDocument> getDocuments() {
		return documents;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
