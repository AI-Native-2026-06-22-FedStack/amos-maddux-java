package com.fedstack.spending.web;

import java.util.List;

/**
 * {@code income}, {@code spending}, and {@code netChange} are decimal
 * strings (not JSON numbers) so the exact {@code BigDecimal} values
 * round-trip without binary floating-point precision loss, matching the
 * {@code Money} contract schema.
 */
public record MonthlyDashboardResponse(
		String month,
		String income,
		String spending,
		String netChange,
		List<DashboardTransactionResponse> transactions
) {
}
