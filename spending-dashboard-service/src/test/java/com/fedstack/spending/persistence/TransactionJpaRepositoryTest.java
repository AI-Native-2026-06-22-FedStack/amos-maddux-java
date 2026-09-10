package com.fedstack.spending.persistence;

import com.fedstack.spending.support.PostgreSqlContainerSupport;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
	private final EntityManager entityManager;

	@Autowired
	TransactionJpaRepositoryTest(
			TransactionJpaRepository transactionRepository,
			JdbcTemplate jdbcTemplate,
			Environment environment,
			EntityManager entityManager
	) {
		this.transactionRepository = transactionRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.environment = environment;
		this.entityManager = entityManager;
	}

	@Test
	void validatesJpaMappingsAgainstSchemaInitializedPostgreSql() {
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(9);
	}

	@Test
	void findsAdaJanuaryTransactionsUsingUserScopeHalfOpenRangeAndDeterministicOrder() {
		List<TransactionEntity> transactions = transactionRepository.findMonthlyDashboardTransactions(
				1L,
				LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-01")
		);

		assertThat(transactions).extracting(TransactionEntity::getId)
				.containsExactly(4L, 5L, 3L, 2L, 1L);
		assertThat(transactions).extracting(TransactionEntity::getId)
				.contains(1L)
				.doesNotContain(6L, 7L, 8L, 9L);
		assertThat(transactions).allSatisfy(transaction -> {
			assertThat(transaction.getAccount().getUser().getId()).isEqualTo(1L);
			assertThat(transaction.getOccurredOn().compareTo(LocalDate.parse("2026-01-01"))).isGreaterThanOrEqualTo(0);
			assertThat(transaction.getOccurredOn().compareTo(LocalDate.parse("2026-02-01"))).isLessThan(0);
		});
		assertThat(transactions.get(2).getId()).isEqualTo(3L);
		assertThat(transactions.get(3).getId()).isEqualTo(2L);
	}

	@Test
	void returnsEmptyMonthlyResultForUserWithNoTransactions() {
		List<TransactionEntity> transactions = transactionRepository.findMonthlyDashboardTransactions(
				3L,
				LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-01")
		);

		assertThat(transactions).isEmpty();
	}

	@Test
	void fetchesRequiredToOneReferencesForMonthlyRead() {
		List<TransactionEntity> transactions = transactionRepository.findMonthlyDashboardTransactions(
				1L,
				LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-01")
		);

		assertThat(transactions).hasSize(5);
		assertThat(transactions).allSatisfy(transaction -> {
			assertThat(Hibernate.isInitialized(transaction.getAccount())).isTrue();
			assertThat(Hibernate.isInitialized(transaction.getAccount().getUser())).isTrue();
			assertThat(Hibernate.isInitialized(transaction.getMerchant())).isTrue();
			assertThat(Hibernate.isInitialized(transaction.getCategory())).isTrue();
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

	@Test
	void roundTripsRequiredEntityGraphThroughPostgreSql() {
		OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-05T14:30:00Z");
		UserEntity user = new UserEntity("roundtrip@example.com", "Round Trip", createdAt);
		AccountEntity account = new AccountEntity(
				user,
				"Round Trip Checking",
				"CHECKING",
				LocalDate.parse("2026-03-01"),
				createdAt
		);
		MerchantEntity merchant = new MerchantEntity("Round Trip Market", createdAt);
		CategoryEntity category = new CategoryEntity("Round Trip Supplies", createdAt);

		entityManager.persist(user);
		entityManager.persist(account);
		entityManager.persist(merchant);
		entityManager.persist(category);

		TransactionEntity saved = transactionRepository.save(new TransactionEntity(
				account,
				merchant,
				category,
				new BigDecimal("123.45"),
				"DEBIT",
				LocalDate.parse("2026-03-15"),
				"Round-trip mapped transaction",
				createdAt
		));
		Long transactionId = saved.getId();

		entityManager.flush();
		entityManager.clear();

		TransactionEntity reloaded = entityManager.find(TransactionEntity.class, transactionId);

		assertThat(reloaded).isNotNull();
		assertThat(reloaded.getId()).isEqualTo(transactionId);
		assertThat(reloaded.getAmount()).isEqualByComparingTo("123.45");
		assertThat(reloaded.getAmount().scale()).isEqualTo(2);
		assertThat(reloaded.getDirection()).isEqualTo("DEBIT");
		assertThat(reloaded.getOccurredOn()).isEqualTo(LocalDate.parse("2026-03-15"));
		assertThat(reloaded.getDescription()).isEqualTo("Round-trip mapped transaction");
		assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);

		assertThat(reloaded.getAccount().getId()).isEqualTo(account.getId());
		assertThat(reloaded.getAccount().getName()).isEqualTo("Round Trip Checking");
		assertThat(reloaded.getAccount().getAccountType()).isEqualTo("CHECKING");
		assertThat(reloaded.getAccount().getOpenedOn()).isEqualTo(LocalDate.parse("2026-03-01"));
		assertThat(reloaded.getAccount().getUser().getId()).isEqualTo(user.getId());
		assertThat(reloaded.getAccount().getUser().getEmail()).isEqualTo("roundtrip@example.com");
		assertThat(reloaded.getAccount().getUser().getDisplayName()).isEqualTo("Round Trip");
		assertThat(reloaded.getMerchant().getId()).isEqualTo(merchant.getId());
		assertThat(reloaded.getMerchant().getName()).isEqualTo("Round Trip Market");
		assertThat(reloaded.getCategory().getId()).isEqualTo(category.getId());
		assertThat(reloaded.getCategory().getName()).isEqualTo("Round Trip Supplies");
	}
}
