package com.fedstack.spending.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CreateTransactionCommand(
		long accountId,
		long merchantId,
		long categoryId,
		BigDecimal amount,
		String direction,
		LocalDate occurredOn,
		String description
) {
	public CreateTransactionCommand {
		if (accountId <= 0) {
			throw new IllegalArgumentException("accountId must be positive");
		}
		if (merchantId <= 0) {
			throw new IllegalArgumentException("merchantId must be positive");
		}
		if (categoryId <= 0) {
			throw new IllegalArgumentException("categoryId must be positive");
		}
		amount = requireTwoDecimalPositiveMoney(amount);
		direction = requireDirection(direction);
		occurredOn = Objects.requireNonNull(occurredOn, "occurredOn must not be null");
		description = requireText(description, "description");
	}

	private static BigDecimal requireTwoDecimalPositiveMoney(BigDecimal value) {
		Objects.requireNonNull(value, "amount must not be null");
		if (value.signum() <= 0) {
			throw new IllegalArgumentException("amount must be positive");
		}
		if (value.scale() != 2) {
			throw new IllegalArgumentException("amount must use exactly two decimal places");
		}
		return value;
	}

	private static String requireDirection(String value) {
		String trimmed = requireText(value, "direction");
		if (!trimmed.equals("CREDIT") && !trimmed.equals("DEBIT")) {
			throw new IllegalArgumentException("direction must be CREDIT or DEBIT");
		}
		return trimmed;
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
