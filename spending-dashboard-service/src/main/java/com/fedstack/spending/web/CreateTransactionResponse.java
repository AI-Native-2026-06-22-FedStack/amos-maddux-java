package com.fedstack.spending.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * {@code amount} is a decimal string (not a JSON number) so the exact
 * two-fraction-digit {@code BigDecimal} value round-trips without binary
 * floating-point precision loss, matching the {@code Money} contract schema.
 */
public record CreateTransactionResponse(
		long id,
		long accountId,
		long merchantId,
		long categoryId,
		String amount,
		String direction,
		LocalDate occurredOn,
		String description,
		OffsetDateTime createdAt
) {
}
