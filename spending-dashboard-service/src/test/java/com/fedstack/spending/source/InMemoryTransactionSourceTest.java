package com.fedstack.spending.source;

import com.fedstack.spending.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InMemoryTransactionSourceTest {
	private final InMemoryTransactionSource source = new InMemoryTransactionSource();

	@Test
	void filtersTheRequestedUserMonthAndOrdersNewestOccurrenceThenDescendingId() {
		List<Transaction> transactions = source.findMonthlyTransactions(1, YearMonth.of(2026, 8));

		assertThat(transactions).extracting(Transaction::id).containsExactly(1004L, 1003L, 1002L, 1001L);
		assertThat(transactions).allSatisfy(transaction -> {
			assertThat(transaction.userId()).isEqualTo(1);
			assertThat(YearMonth.from(transaction.occurredOn())).isEqualTo(YearMonth.of(2026, 8));
		});
	}

	@Test
	void excludesOtherUsersAndOtherMonths() {
		List<Transaction> transactions = source.findMonthlyTransactions(1, YearMonth.of(2026, 8));

		assertThat(transactions).extracting(Transaction::id)
				.doesNotContain(1005L, 1006L);
	}

	@Test
	void returnsAnImmutableResult() {
		List<Transaction> transactions = source.findMonthlyTransactions(1, YearMonth.of(2026, 8));

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> transactions.add(transactions.getFirst()));
	}
}
