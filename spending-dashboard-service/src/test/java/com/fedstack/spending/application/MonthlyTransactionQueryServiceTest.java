package com.fedstack.spending.application;

import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.domain.TransactionType;
import com.fedstack.spending.source.TransactionSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyTransactionQueryServiceTest {
	private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

	@Test
	void returnsTransactionsInTheSourceOrderWithoutSortingAgain() {
		List<Transaction> sourceOrder = List.of(
				transaction(7, "2026-08-01"),
				transaction(3, "2026-08-20"),
				transaction(9, "2026-08-12")
		);
		MonthlyTransactionQueryService service = new MonthlyTransactionQueryService((userId, month) -> sourceOrder);

		List<Transaction> transactions = service.findMonthlyTransactions(1, AUGUST_2026);

		assertThat(transactions).containsExactlyElementsOf(sourceOrder);
		assertThat(transactions).extracting(Transaction::id).containsExactly(7L, 3L, 9L);
	}

	@Test
	void asksTheSourceForOnlyTheRequestedUserMonth() {
		RecordingSource source = new RecordingSource(List.of());
		MonthlyTransactionQueryService service = new MonthlyTransactionQueryService(source);

		service.findMonthlyTransactions(9, AUGUST_2026);

		assertThat(source.requestedUserId).isEqualTo(9);
		assertThat(source.requestedMonth).isEqualTo(AUGUST_2026);
	}

	private static Transaction transaction(long id, String occurredOn) {
		return new Transaction(
				id,
				1,
				LocalDate.parse(occurredOn),
				TransactionType.EXPENSE,
				new BigDecimal("10.00"),
				"Test transaction"
		);
	}

	private static class RecordingSource implements TransactionSource {
		private final List<Transaction> transactions;
		private long requestedUserId;
		private YearMonth requestedMonth;

		private RecordingSource(List<Transaction> transactions) {
			this.transactions = transactions;
		}

		@Override
		public List<Transaction> findMonthlyTransactions(long userId, YearMonth month) {
			this.requestedUserId = userId;
			this.requestedMonth = month;
			return transactions;
		}
	}
}
