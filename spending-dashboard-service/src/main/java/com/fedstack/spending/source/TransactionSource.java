package com.fedstack.spending.source;

import com.fedstack.spending.domain.Transaction;

import java.time.YearMonth;
import java.util.List;

public interface TransactionSource {
	/**
	 * Returns the requested user's monthly transactions newest occurrence first,
	 * using descending identifiers only to break equal-occurrence ties.
	 */
	List<Transaction> findMonthlyTransactions(long userId, YearMonth month);
}
