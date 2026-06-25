package com.br.auction.garage.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.br.auction.garage.enums.ExpenseStatus;
import com.br.auction.garage.enums.ExpenseType;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "tbAcquisitionExpense")
@Schema(description = "Gasto associado a um veiculo adquirido. Pode comecar como cotacao e virar despesa quando comprado.")
public class AcquisitionExpense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fk_Id_Acquisition", foreignKey = @ForeignKey(name = "FK_FROM_TBACQUISITIONEXPENSE_FOR_TBACQUISITION"))
	private Acquisition acquisition;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	@Schema(description = "Tipo do gasto")
	private ExpenseType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Schema(description = "Situacao do gasto: cotacao ou comprado")
	private ExpenseStatus status = ExpenseStatus.COMPRADO;

	@Column(length = 300)
	@Schema(description = "Descricao do gasto")
	private String description;

	@Column(name = "amount", precision = 15, scale = 2)
	@Schema(description = "Valor efetivo (definido quando comprado)")
	private BigDecimal value;

	@Column(length = 200)
	@Schema(description = "Local onde foi comprado")
	private String place;

	@Column
	@Schema(description = "Data do gasto")
	private LocalDate incurredAt;

	@OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("value ASC")
	@Schema(description = "Orcamentos (cotacoes) deste gasto")
	private List<ExpenseQuote> quotes = new ArrayList<>();

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.incurredAt == null) {
			this.incurredAt = LocalDate.now();
		}
		if (this.status == null) {
			this.status = ExpenseStatus.COMPRADO;
		}
	}

	public void addQuote(ExpenseQuote quote) {
		quote.setExpense(this);
		this.quotes.add(quote);
	}

	public boolean isPurchased() {
		return status == ExpenseStatus.COMPRADO;
	}

	/**
	 * Valor que conta para os totais: somente gastos comprados entram na conta.
	 */
	public BigDecimal effectiveValue() {
		if (status != ExpenseStatus.COMPRADO || value == null) {
			return BigDecimal.ZERO;
		}
		return value;
	}

	public Long getId() {
		return id;
	}

	public Acquisition getAcquisition() {
		return acquisition;
	}

	public void setAcquisition(Acquisition acquisition) {
		this.acquisition = acquisition;
	}

	public ExpenseType getType() {
		return type;
	}

	public void setType(ExpenseType type) {
		this.type = type;
	}

	public ExpenseStatus getStatus() {
		return status;
	}

	public void setStatus(ExpenseStatus status) {
		this.status = status;
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

	public List<ExpenseQuote> getQuotes() {
		return quotes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
