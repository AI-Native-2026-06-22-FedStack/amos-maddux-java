package com.fedstack.spending.application;

import com.fedstack.spending.domain.MonthlySummary;
import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.domain.TransactionType;
import com.fedstack.spending.source.TransactionSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlySummaryServiceTest {
	private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

	@Test
	void calculatesIncomeSpendingAndNetChangeForSourceTransactions() {
		MonthlySummaryService service = new MonthlySummaryService(sourceReturning(List.of(
				transaction(1, 1, "2026-08-01", TransactionType.INCOME, "2500.00"),
				transaction(2, 1, "2026-08-02", TransactionType.INCOME, "2500.00"),
				transaction(3, 1, "2026-08-03", TransactionType.EXPENSE, "1500.00"),
				transaction(4, 1, "2026-08-04", TransactionType.EXPENSE, "226.00")
		)));

		MonthlySummary summary = service.summarizeMonth(1, AUGUST_2026);

		assertThat(summary.income()).isEqualByComparingTo("5000.00");
		assertThat(summary.spending()).isEqualByComparingTo("1726.00");
		assertThat(summary.netChange()).isEqualByComparingTo("3274.00");
	}

	@Test
	void asksTheSourceForOnlyTheRequestedUserMonth() {
		RecordingSource source = new RecordingSource(List.of());
		MonthlySummaryService service = new MonthlySummaryService(source);

		service.summarizeMonth(42, AUGUST_2026);

		assertThat(source.requestedUserId).isEqualTo(42);
		assertThat(source.requestedMonth).isEqualTo(AUGUST_2026);
	}

	@Test
	void returnsZeroSummaryWhenTheSourceFindsNoTransactions() {
		MonthlySummaryService service = new MonthlySummaryService(sourceReturning(List.of()));

		MonthlySummary summary = service.summarizeMonth(1, AUGUST_2026);

		assertThat(summary.income()).isEqualByComparingTo("0.00");
		assertThat(summary.spending()).isEqualByComparingTo("0.00");
		assertThat(summary.netChange()).isEqualByComparingTo("0.00");
	}

	private static TransactionSource sourceReturning(List<Transaction> transactions) {
		return new RecordingSource(transactions);
	}

	private static Transaction transaction(
			long id,
			long userId,
			String occurredOn,
			TransactionType type,
			String amount
	) {
		return new Transaction(
				id,
				userId,
				LocalDate.parse(occurredOn),
				type,
				new BigDecimal(amount),
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
