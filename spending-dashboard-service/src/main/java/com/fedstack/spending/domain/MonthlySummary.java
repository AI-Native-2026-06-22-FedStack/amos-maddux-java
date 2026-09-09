package com.fedstack.spending.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record MonthlySummary(BigDecimal income, BigDecimal spending, BigDecimal netChange) {
	public MonthlySummary {
		income = requireTwoDecimalMoney(income, "income");
		spending = requireTwoDecimalMoney(spending, "spending");
		netChange = requireTwoDecimalAmount(netChange, "netChange");
	}

	private static BigDecimal requireTwoDecimalMoney(BigDecimal value, String fieldName) {
		BigDecimal checked = requireTwoDecimalAmount(value, fieldName);
		if (checked.signum() < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return checked;
	}

	private static BigDecimal requireTwoDecimalAmount(BigDecimal value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		if (value.scale() != 2) {
			throw new IllegalArgumentException(fieldName + " must use exactly two decimal places");
		}
		return value;
	}
}
