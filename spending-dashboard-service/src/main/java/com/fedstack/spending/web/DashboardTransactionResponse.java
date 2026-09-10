package com.fedstack.spending.web;

import com.fedstack.spending.domain.TransactionType;

import java.time.LocalDate;

/**
 * {@code amount} is a decimal string (not a JSON number) so the exact
 * two-fraction-digit {@code BigDecimal} value round-trips without binary
 * floating-point precision loss, matching the {@code Money} contract schema.
 */
public record DashboardTransactionResponse(
		long id,
		LocalDate occurredOn,
		TransactionType type,
		String amount,
		String description
) {
}
