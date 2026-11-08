package com.saas.procurement.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money is always BigDecimal with 2 decimals - never double. */
public final class Money {

	private Money() {
	}

	public static BigDecimal total(BigDecimal unitPrice, int quantity) {
		return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
	}

	public static BigDecimal scale(BigDecimal value) {
		return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
	}

}
