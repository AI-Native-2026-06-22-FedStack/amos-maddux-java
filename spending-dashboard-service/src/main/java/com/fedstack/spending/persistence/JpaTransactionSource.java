package com.fedstack.spending.persistence;

import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.domain.TransactionType;
import com.fedstack.spending.source.TransactionSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Repository
@Profile("!in-memory")
public class JpaTransactionSource implements TransactionSource {
	private static final String CREDIT = "CREDIT";
	private static final String DEBIT = "DEBIT";

	private final TransactionJpaRepository transactionRepository;

	public JpaTransactionSource(TransactionJpaRepository transactionRepository) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository,
				"transactionRepository must not be null"
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Transaction> findMonthlyTransactions(long userId, YearMonth month) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Objects.requireNonNull(month, "month must not be null");

		LocalDate start = month.atDay(1);
		LocalDate end = month.plusMonths(1).atDay(1);

		return transactionRepository.findMonthlyDashboardTransactions(userId, start, end).stream()
				.map(JpaTransactionSource::toDomain)
				.toList();
	}

	private static Transaction toDomain(TransactionEntity transaction) {
		return new Transaction(
				transaction.getId(),
				transaction.getAccount().getUser().getId(),
				transaction.getOccurredOn(),
				toTransactionType(transaction.getDirection()),
				transaction.getAmount(),
				transaction.getDescription()
		);
	}

	private static TransactionType toTransactionType(String direction) {
		return switch (direction) {
			case CREDIT -> TransactionType.INCOME;
			case DEBIT -> TransactionType.EXPENSE;
			default -> throw new IllegalStateException("Unsupported transaction direction: " + direction);
		};
	}
}
