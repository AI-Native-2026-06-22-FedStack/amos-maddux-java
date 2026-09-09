package com.fedstack.spending;

import com.fedstack.spending.application.MonthlySummaryService;
import com.fedstack.spending.application.MonthlyTransactionQueryService;
import com.fedstack.spending.console.DashboardConsoleRunner;
import com.fedstack.spending.source.TransactionSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SpendingDashboardServiceApplicationTests {
	private final ApplicationContext context;
	private final MonthlySummaryService summaryService;
	private final MonthlyTransactionQueryService transactionQueryService;

	@Autowired
	SpendingDashboardServiceApplicationTests(
			ApplicationContext context,
			MonthlySummaryService summaryService,
			MonthlyTransactionQueryService transactionQueryService
	) {
		this.context = context;
		this.summaryService = summaryService;
		this.transactionQueryService = transactionQueryService;
	}

	@Test
	void contextDiscoversRequiredApplicationBeans() {
		assertThat(context.getBeansOfType(TransactionSource.class)).hasSize(1);
		assertThat(context.getBeansOfType(MonthlySummaryService.class)).hasSize(1);
		assertThat(context.getBeansOfType(MonthlyTransactionQueryService.class)).hasSize(1);
		assertThat(context.getBeansOfType(DashboardConsoleRunner.class)).hasSize(1);

		assertThat(summaryService).isSameAs(context.getBean(MonthlySummaryService.class));
		assertThat(transactionQueryService).isSameAs(context.getBean(MonthlyTransactionQueryService.class));
		assertThat(context.getBean(DashboardConsoleRunner.class)).isInstanceOf(CommandLineRunner.class);
	}
}
