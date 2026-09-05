package com.fedstack.spending.application;

import com.fedstack.spending.domain.MonthlySummary;
import com.fedstack.spending.domain.Transaction;
import com.fedstack.spending.domain.TransactionType;
import com.fedstack.spending.source.TransactionSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

@Service
public class MonthlySummaryService {
	private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

	private final TransactionSource transactionSource;

	public MonthlySummaryService(TransactionSource transactionSource) {
		this.transactionSource = Objects.requireNonNull(transactionSource, "transactionSource must not be null");
	}

	public MonthlySummary summarizeMonth(long userId, YearMonth month) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Objects.requireNonNull(month, "month must not be null");

		BigDecimal income = ZERO_MONEY;
		BigDecimal spending = ZERO_MONEY;
		for (Transaction transaction : transactionSource.findMonthlyTransactions(userId, month)) {
			if (transaction.type() == TransactionType.INCOME) {
				income = income.add(transaction.amount());
			}
			if (transaction.type() == TransactionType.EXPENSE) {
				spending = spending.add(transaction.amount());
			}
		}
		return new MonthlySummary(income, spending, income.subtract(spending));
	}
}
