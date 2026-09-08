package com.fedstack.spending.persistence;

import com.fedstack.spending.support.PostgreSqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class TransactionJpaRepositoryTest extends PostgreSqlContainerSupport {
	private final TransactionJpaRepository transactionRepository;
	private final JdbcTemplate jdbcTemplate;
	private final Environment environment;

	@Autowired
	TransactionJpaRepositoryTest(
			TransactionJpaRepository transactionRepository,
			JdbcTemplate jdbcTemplate,
			Environment environment
	) {
		this.transactionRepository = transactionRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.environment = environment;
	}

	@Test
	void validatesJpaMappingsAgainstSchemaInitializedPostgreSql() {
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(9);
	}

	@Test
	void findsSeededMonthlyDashboardTransactionsForOneUserInDeterministicOrder() {
		List<TransactionEntity> transactions = transactionRepository.findMonthlyDashboardTransactions(
				1L,
				LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-01")
		);

		assertThat(transactions).extracting(TransactionEntity::getId)
				.containsExactly(4L, 5L, 3L, 2L, 1L);
		assertThat(transactions).allSatisfy(transaction -> {
			assertThat(transaction.getAccount().getUser().getId()).isEqualTo(1L);
			assertThat(transaction.getOccurredOn().compareTo(LocalDate.parse("2026-01-01"))).isGreaterThanOrEqualTo(0);
			assertThat(transaction.getOccurredOn().compareTo(LocalDate.parse("2026-02-01"))).isLessThan(0);
		});
		assertThat(transactions).extracting(transaction -> transaction.getMerchant().getName())
				.containsExactly(
						"Acme Payroll",
						"City Power & Light",
						"Metro Transit",
						"Whole Foods Market",
						"Blue Bottle Coffee"
				);
		assertThat(transactions).extracting(transaction -> transaction.getCategory().getName())
				.containsExactly("Income", "Utilities", "Transport", "Groceries", "Dining");
	}
}
