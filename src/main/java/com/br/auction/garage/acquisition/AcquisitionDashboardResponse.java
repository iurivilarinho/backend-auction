package com.br.auction.garage.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.garage.enums.ExpenseType;
import com.br.auction.garage.models.Acquisition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores da garagem: gastos, vendas e lucro dos veiculos adquiridos")
public class AcquisitionDashboardResponse {

	private final long totalAcquisitions;
	private final long soldCount;
	private final long inStockCount;
	private final long upcomingInspections;
	private final BigDecimal totalAcquisitionValue;
	private final BigDecimal totalExpenses;
	private final BigDecimal totalInvested;
	private final BigDecimal totalSaleValue;
	private final BigDecimal totalProfit;
	private final Map<String, Long> byStatus;
	private final Map<String, BigDecimal> expensesByType;

	public AcquisitionDashboardResponse(List<Acquisition> acquisitions) {
		this.totalAcquisitions = acquisitions.size();

		Map<String, Long> statusCounts = new LinkedHashMap<>();
		for (AcquisitionStatus status : AcquisitionStatus.values()) {
			statusCounts.put(status.name(), 0L);
		}
		Map<String, BigDecimal> expenseTotals = new LinkedHashMap<>();
		for (ExpenseType type : ExpenseType.values()) {
			expenseTotals.put(type.name(), BigDecimal.ZERO);
		}

		BigDecimal acquisitionSum = BigDecimal.ZERO;
		BigDecimal expensesSum = BigDecimal.ZERO;
		BigDecimal saleSum = BigDecimal.ZERO;
		BigDecimal profitSum = BigDecimal.ZERO;
		long sold = 0;
		long inStock = 0;
		long inspections = 0;
		LocalDate today = LocalDate.now();

		for (Acquisition acquisition : acquisitions) {
			AcquisitionStatus status = acquisition.getStatus();
			if (status != null) {
				statusCounts.merge(status.name(), 1L, Long::sum);
			}
			BigDecimal acqValue = nullToZero(acquisition.getAcquisitionValue());
			acquisitionSum = acquisitionSum.add(acqValue);

			BigDecimal itemExpenses = BigDecimal.ZERO;
			for (var expense : acquisition.getExpenses()) {
				BigDecimal value = expense.effectiveValue();
				itemExpenses = itemExpenses.add(value);
				if (expense.getType() != null) {
					expenseTotals.merge(expense.getType().name(), value, BigDecimal::add);
				}
			}
			expensesSum = expensesSum.add(itemExpenses);

			BigDecimal invested = acqValue.add(itemExpenses);
			if (acquisition.getSaleValue() != null) {
				saleSum = saleSum.add(acquisition.getSaleValue());
				profitSum = profitSum.add(acquisition.getSaleValue().subtract(invested));
			}
			if (status == AcquisitionStatus.VENDIDO || status == AcquisitionStatus.FINALIZADO) {
				sold++;
			} else {
				inStock++;
			}
			if (acquisition.getInspectionDeadline() != null && !acquisition.getInspectionDeadline().isBefore(today)) {
				inspections++;
			}
		}

		this.soldCount = sold;
		this.inStockCount = inStock;
		this.upcomingInspections = inspections;
		this.totalAcquisitionValue = acquisitionSum;
		this.totalExpenses = expensesSum;
		this.totalInvested = acquisitionSum.add(expensesSum);
		this.totalSaleValue = saleSum;
		this.totalProfit = profitSum;
		this.byStatus = statusCounts;
		this.expensesByType = expenseTotals;
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	public long getTotalAcquisitions() {
		return totalAcquisitions;
	}

	public long getSoldCount() {
		return soldCount;
	}

	public long getInStockCount() {
		return inStockCount;
	}

	public long getUpcomingInspections() {
		return upcomingInspections;
	}

	public BigDecimal getTotalAcquisitionValue() {
		return totalAcquisitionValue;
	}

	public BigDecimal getTotalExpenses() {
		return totalExpenses;
	}

	public BigDecimal getTotalInvested() {
		return totalInvested;
	}

	public BigDecimal getTotalSaleValue() {
		return totalSaleValue;
	}

	public BigDecimal getTotalProfit() {
		return totalProfit;
	}

	public Map<String, Long> getByStatus() {
		return byStatus;
	}

	public Map<String, BigDecimal> getExpensesByType() {
		return expensesByType;
	}
}
