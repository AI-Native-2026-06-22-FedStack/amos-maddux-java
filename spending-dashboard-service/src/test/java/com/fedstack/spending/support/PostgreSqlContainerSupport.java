package com.fedstack.spending.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class PostgreSqlContainerSupport {
	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("spending_dashboard")
			.withUsername("spending")
			.withPassword("spending")
			.withInitScript("db/spending_dashboard_schema.sql");

	@DynamicPropertySource
	static void configurePostgreSqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
	}
}
