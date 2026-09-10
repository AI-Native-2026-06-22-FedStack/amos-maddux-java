package com.fedstack.spending.web;

import com.fedstack.spending.application.MonthlySummaryService;
import com.fedstack.spending.application.MonthlyTransactionQueryService;
import com.fedstack.spending.domain.MonthlySummary;
import com.fedstack.spending.domain.Transaction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private final MonthlySummaryService summaryService;
	private final MonthlyTransactionQueryService transactionQueryService;

	public DashboardController(
			MonthlySummaryService summaryService,
			MonthlyTransactionQueryService transactionQueryService
	) {
		this.summaryService = Objects.requireNonNull(summaryService, "summaryService must not be null");
		this.transactionQueryService = Objects.requireNonNull(
				transactionQueryService,
				"transactionQueryService must not be null"
		);
	}

	@GetMapping
	MonthlyDashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt, @RequestParam String month) {
		long userId = Long.parseLong(jwt.getSubject());
		YearMonth requestedMonth = parseMonth(month);

		MonthlySummary summary = summaryService.summarizeMonth(userId, requestedMonth);
		var transactions = transactionQueryService.findMonthlyTransactions(userId, requestedMonth).stream()
				.map(DashboardController::toResponse)
				.toList();

		return new MonthlyDashboardResponse(
				requestedMonth.format(MONTH_FORMAT),
				summary.income().toPlainString(),
				summary.spending().toPlainString(),
				summary.netChange().toPlainString(),
				transactions
		);
	}

	private static YearMonth parseMonth(String month) {
		try {
			return YearMonth.parse(month, MONTH_FORMAT);
		} catch (DateTimeParseException exception) {
			throw new MalformedRequestException("month must match pattern YYYY-MM");
		}
	}

	private static DashboardTransactionResponse toResponse(Transaction transaction) {
		return new DashboardTransactionResponse(
				transaction.id(),
				transaction.occurredOn(),
				transaction.type(),
				transaction.amount().toPlainString(),
				transaction.description()
		);
	}
}
