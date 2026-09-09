package com.fedstack.spending.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Transaction(
		long id,
		long userId,
		LocalDate occurredOn,
		TransactionType type,
		BigDecimal amount,
		String description
) {
	public Transaction {
		if (id <= 0) {
			throw new IllegalArgumentException("transaction id must be positive");
		}
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		occurredOn = Objects.requireNonNull(occurredOn, "occurredOn must not be null");
		type = Objects.requireNonNull(type, "type must not be null");
		amount = requireTwoDecimalMoney(amount, "amount");
		description = requireText(description, "description");
	}

	private static BigDecimal requireTwoDecimalMoney(BigDecimal value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		if (value.signum() < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		if (value.scale() != 2) {
			throw new IllegalArgumentException(fieldName + " must use exactly two decimal places");
		}
		return value;
	}

	private static String requireText(String value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return trimmed;
	}
}
