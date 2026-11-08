package com.saas.procurement.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.saas.procurement.common.Money;
import com.saas.procurement.order.PurchaseOrder;

class ReportServiceTest {

	@Test
	void dailyFillsEmptyDaysWithZero() {
		LocalDate from = LocalDate.of(2026, 11, 1);
		PurchaseOrder a = order(from, "100.50");
		PurchaseOrder b = order(from, "49.50");
		PurchaseOrder c = order(from.plusDays(2), "10.00");

		var points = ReportService.daily(List.of(a, b, c), from, from.plusDays(3));

		assertThat(points).hasSize(4);
		assertThat(points.get(0).orders()).isEqualTo(2);
		assertThat(points.get(0).amount()).isEqualByComparingTo("150.00");
		assertThat(points.get(1).orders()).isZero();
		assertThat(points.get(1).amount()).isEqualByComparingTo("0");
		assertThat(points.get(2).amount()).isEqualByComparingTo("10.00");
	}

	@Test
	void moneyTotalUsesTwoDecimals() {
		assertThat(Money.total(new BigDecimal("18.50"), 3)).isEqualByComparingTo("55.50");
		assertThat(Money.total(new BigDecimal("0.333"), 3).scale()).isEqualTo(2);
	}

	private static PurchaseOrder order(LocalDate day, String amount) {
		PurchaseOrder o = new PurchaseOrder();
		o.setCreatedAt(day.atTime(10, 0));
		o.setTotalAmount(new BigDecimal(amount));
		return o;
	}

}
