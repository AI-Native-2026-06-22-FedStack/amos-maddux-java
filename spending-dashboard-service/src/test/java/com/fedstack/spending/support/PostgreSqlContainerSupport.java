package com.fedstack.spending.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public abstract class PostgreSqlContainerSupport {
	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("spending_dashboard")
			.withUsername("spending")
			.withPassword("spending")
			.withCopyFileToContainer(
					MountableFile.forClasspathResource("db/spending_dashboard_schema.sql"),
					"/docker-entrypoint-initdb.d/01_spending_dashboard_schema.sql"
			)
			.withCopyFileToContainer(
					MountableFile.forClasspathResource("db/spending_dashboard_seed.sql"),
					"/docker-entrypoint-initdb.d/02_spending_dashboard_seed.sql"
			);

	@DynamicPropertySource
	static void configurePostgreSqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRESQL::getUsername);
		registry.add("spring.datasource.password", POSTGRESQL::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
	}
}
