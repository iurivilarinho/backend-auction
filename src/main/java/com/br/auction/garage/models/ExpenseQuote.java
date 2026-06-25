package com.br.auction.garage.models;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Orcamento de um gasto: o valor cotado em um determinado local. Quando o usuario marca um
 * orcamento como comprado, o gasto vira despesa efetiva com este valor.
 */
@Entity
@Table(name = "tbExpenseQuote")
@Schema(description = "Orcamento (cotacao) de um gasto em um local")
public class ExpenseQuote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fk_Id_Expense", foreignKey = @ForeignKey(name = "FK_FROM_TBEXPENSEQUOTE_FOR_TBACQUISITIONEXPENSE"))
	private AcquisitionExpense expense;

	@Column(nullable = false, length = 200)
	@Schema(description = "Local/fornecedor do orcamento")
	private String place;

	@Column(nullable = false, precision = 15, scale = 2)
	@Schema(description = "Valor orcado")
	private BigDecimal value;

	@Column(length = 300)
	@Schema(description = "Observacoes do orcamento")
	private String notes;

	@Column(nullable = false)
	@Schema(description = "Indica se este orcamento foi o escolhido (comprado)")
	private Boolean selected = Boolean.FALSE;

	public Long getId() {
		return id;
	}

	public AcquisitionExpense getExpense() {
		return expense;
	}

	public void setExpense(AcquisitionExpense expense) {
		this.expense = expense;
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

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}
}
