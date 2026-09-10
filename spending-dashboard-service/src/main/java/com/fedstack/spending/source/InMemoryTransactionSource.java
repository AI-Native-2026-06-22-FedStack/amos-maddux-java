package com.fedstack.spending.source;

import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.domain.TransactionType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
@Profile("in-memory")
public class InMemoryTransactionSource implements TransactionSource {
	private static final Comparator<Transaction> MONTHLY_ORDER =
			Comparator.comparing(Transaction::occurredOn, Comparator.reverseOrder())
					.thenComparing(Comparator.comparingLong(Transaction::id).reversed());

	private final List<Transaction> transactions;

	public InMemoryTransactionSource() {
		this.transactions = List.of(
				transaction(1001, 1, "2026-08-01", TransactionType.INCOME, "5000.00", "Salary"),
				transaction(1002, 1, "2026-08-03", TransactionType.EXPENSE, "1500.00", "Rent"),
				transaction(1003, 1, "2026-08-12", TransactionType.EXPENSE, "180.75", "Groceries"),
				transaction(1004, 1, "2026-08-12", TransactionType.EXPENSE, "45.25", "Transit"),
				transaction(1005, 1, "2026-09-01", TransactionType.EXPENSE, "80.00", "Internet"),
				transaction(1006, 2, "2026-08-14", TransactionType.INCOME, "4000.00", "Salary")
		);
	}

	@Override
	public List<Transaction> findMonthlyTransactions(long userId, YearMonth month) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Objects.requireNonNull(month, "month must not be null");

		return transactions.stream()
				.filter(transaction -> transaction.userId() == userId)
				.filter(transaction -> YearMonth.from(transaction.occurredOn()).equals(month))
				.sorted(MONTHLY_ORDER)
				.toList();
	}

	private static Transaction transaction(
			long id,
			long userId,
			String occurredOn,
			TransactionType type,
			String amount,
			String description
	) {
		return new Transaction(
				id,
				userId,
				LocalDate.parse(occurredOn),
				type,
				new BigDecimal(amount),
				description
		);
	}
}
