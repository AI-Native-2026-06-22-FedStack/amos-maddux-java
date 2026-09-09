package com.fedstack.spending.application;

import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.source.TransactionSource;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
public class MonthlyTransactionQueryService {
	private final TransactionSource transactionSource;

	public MonthlyTransactionQueryService(TransactionSource transactionSource) {
		this.transactionSource = Objects.requireNonNull(transactionSource, "transactionSource must not be null");
	}

	public List<Transaction> findMonthlyTransactions(long userId, YearMonth month) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Objects.requireNonNull(month, "month must not be null");

		return transactionSource.findMonthlyTransactions(userId, month);
	}
}
