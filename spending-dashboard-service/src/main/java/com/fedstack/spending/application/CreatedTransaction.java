package com.fedstack.spending.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Plain application result for a transaction created by
 * {@link TransactionCreationService}.
 *
 * <p>This keeps persistence entities inside the application/persistence
 * boundary and gives web adapters only the data needed to build a response.</p>
 */
public record CreatedTransaction(
		long id,
		long accountId,
		long merchantId,
		long categoryId,
		BigDecimal amount,
		String direction,
		LocalDate occurredOn,
		String description,
		OffsetDateTime createdAt
) {
}
