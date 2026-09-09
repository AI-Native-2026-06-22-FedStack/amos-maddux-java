package com.fedstack.spending.console;

import com.fedstack.spending.application.MonthlySummaryService;
import com.fedstack.spending.application.MonthlyTransactionQueryService;
import com.fedstack.spending.domain.MonthlySummary;
import com.fedstack.spending.domain.Transaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DashboardConsoleRunner implements CommandLineRunner {
	private static final long DEMO_USER_ID = 1;
	private static final YearMonth DEMO_MONTH = YearMonth.of(2026, 8);

	private final MonthlySummaryService summaryService;
	private final MonthlyTransactionQueryService transactionQueryService;

	public DashboardConsoleRunner(
			MonthlySummaryService summaryService,
			MonthlyTransactionQueryService transactionQueryService
	) {
		this.summaryService = Objects.requireNonNull(summaryService, "summaryService must not be null");
		this.transactionQueryService = Objects.requireNonNull(
				transactionQueryService,
				"transactionQueryService must not be null"
		);
	}

	@Override
	public void run(String... args) {
		MonthlySummary summary = summaryService.summarizeMonth(DEMO_USER_ID, DEMO_MONTH);
		String transactionIds = transactionQueryService.findMonthlyTransactions(DEMO_USER_ID, DEMO_MONTH).stream()
				.map(Transaction::id)
				.map(String::valueOf)
				.collect(Collectors.joining(", "));

		System.out.printf("Monthly summary for user %d in %s%n", DEMO_USER_ID, DEMO_MONTH);
		System.out.printf("income:     %s%n", summary.income());
		System.out.printf("spending:   %s%n", summary.spending());
		System.out.printf("net change: %s%n", summary.netChange());
		System.out.printf("transaction IDs: %s%n", transactionIds);
	}
}
