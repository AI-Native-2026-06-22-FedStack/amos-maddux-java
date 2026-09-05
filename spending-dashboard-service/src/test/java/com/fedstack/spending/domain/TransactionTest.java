package com.fedstack.spending.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TransactionTest {
	@Test
	void trimsDescription() {
		Transaction transaction = new Transaction(
				1,
				1,
				LocalDate.parse("2026-08-01"),
				TransactionType.INCOME,
				new BigDecimal("10.00"),
				" Salary "
		);

		assertThat(transaction.description()).isEqualTo("Salary");
	}

	@Test
	void rejectsNegativeMoney() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Transaction(
						1,
						1,
						LocalDate.parse("2026-08-01"),
						TransactionType.INCOME,
						new BigDecimal("-1.00"),
						"Salary"
				));
	}

	@Test
	void rejectsMoneyWithoutExactlyTwoDecimalPlaces() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Transaction(
						1,
						1,
						LocalDate.parse("2026-08-01"),
						TransactionType.INCOME,
						new BigDecimal("1.0"),
						"Salary"
				));
	}
}
