package com.fedstack.spending.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedstack.spending.auth.token.JwtAuthProperties;
import com.fedstack.spending.support.JwtTestSupport;
import com.fedstack.spending.support.OpenApiContractSupport;
import com.fedstack.spending.support.PostgreSqlContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TransactionApiHttpTest extends PostgreSqlContainerSupport {
	private static final long ADA_USER_ID = 1L;
	private static final long BEN_USER_ID = 2L;
	private static final long CLEO_USER_ID = 3L;

	private static final long HIGHEST_SEEDED_TRANSACTION_ID = 9L;

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final JwtTestSupport jwtTestSupport;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	TransactionApiHttpTest(
			MockMvc mockMvc,
			ObjectMapper objectMapper,
			JwtAuthProperties jwtProperties,
			JwtEncoder jwtEncoder,
			JdbcTemplate jdbcTemplate
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.jwtTestSupport = new JwtTestSupport(jwtProperties, jwtEncoder);
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	void restoreSeededTransactions() {
		jdbcTemplate.update("delete from transactions where id > ?", HIGHEST_SEEDED_TRANSACTION_ID);
	}

	@Test
	void returnsAdasJanuaryDashboardOrderedNewestFirst() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/dashboard")
						.param("month", "2026-01")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.month").value("2026-01"))
				.andExpect(jsonPath("$.income").value("2400.00"))
				.andExpect(jsonPath("$.spending").value("186.20"))
				.andExpect(jsonPath("$.netChange").value("2213.80"))
				.andExpect(jsonPath("$.transactions[0].id").value(4))
				.andExpect(jsonPath("$.transactions[1].id").value(5))
				.andExpect(jsonPath("$.transactions[2].id").value(3))
				.andExpect(jsonPath("$.transactions[3].id").value(2))
				.andExpect(jsonPath("$.transactions[4].id").value(1))
				.andExpect(jsonPath("$.transactions.length()").value(5))
				.andReturn();

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/dashboard", "get", 200, "application/json", result.getResponse().getContentAsString());
	}

	@Test
	void returnsEmptyDashboardForUserWithNoTransactionsInMonth() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard")
						.param("month", "2026-01")
						.header(HttpHeaders.AUTHORIZATION, bearer(CLEO_USER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.income").value("0.00"))
				.andExpect(jsonPath("$.spending").value("0.00"))
				.andExpect(jsonPath("$.netChange").value("0.00"))
				.andExpect(jsonPath("$.transactions.length()").value(0));
	}

	@Test
	void doesNotLeakOneUsersTransactionsIntoAnothersDashboard() throws Exception {
		JsonNode adaDashboard = readJson(mockMvc.perform(get("/api/v1/dashboard")
						.param("month", "2026-01")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID)))
				.andExpect(status().isOk())
				.andReturn());

		for (JsonNode transaction : adaDashboard.path("transactions")) {
			assertThat(transaction.path("id").asLong()).isNotIn(8L, 9L);
		}
	}

	@Test
	void rejectsMalformedMonthWithProblemDetails() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/dashboard")
						.param("month", "not-a-month")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID)))
				.andExpect(status().isBadRequest())
				.andExpect(result2 -> assertThat(result2.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Malformed Request"))
				.andReturn();

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/dashboard", "get", 400, "application/problem+json", result.getResponse().getContentAsString());
	}

	@Test
	void rejectsDashboardRequestWithoutToken() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-01"))
				.andExpect(status().isUnauthorized())
				.andExpect(result2 -> assertThat(result2.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andReturn();

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/dashboard", "get", 401, "application/problem+json", result.getResponse().getContentAsString());
	}

	@Test
	void createsTransactionAndReturnsLocationAndDecimalSafeBody() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 1,
				"merchantId", 1,
				"categoryId", 5,
				"amount", "12.34",
				"direction", "DEBIT",
				"occurredOn", "2026-01-22",
				"description", "Afternoon coffee"
		));

		MvcResult result = mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andExpect(jsonPath("$.amount").value("12.34"))
				.andExpect(jsonPath("$.direction").value("DEBIT"))
				.andExpect(jsonPath("$.description").value("Afternoon coffee"))
				.andReturn();

		String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
		assertThat(location).matches(".*/api/v1/transactions/\\d+");

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/transactions", "post", 201, "application/json", result.getResponse().getContentAsString());
	}

	@Test
	void rejectsTransactionValidationFailureWithProblemDetails() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 1,
				"merchantId", 1,
				"categoryId", 5,
				"amount", "-5.00",
				"direction", "DEBIT",
				"occurredOn", "2026-01-22",
				"description", "Invalid amount"
		));

		MvcResult result = mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(result2 -> assertThat(result2.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.status").value(400))
				.andReturn();

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/transactions", "post", 400, "application/problem+json", result.getResponse().getContentAsString());
	}

	@Test
	void rejectsTransactionForAccountNotOwnedByCaller() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 3,
				"merchantId", 1,
				"categoryId", 5,
				"amount", "10.00",
				"direction", "DEBIT",
				"occurredOn", "2026-01-22",
				"description", "Not Ada's account"
		));

		mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isForbidden())
				.andExpect(result -> assertThat(result.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void rejectsTransactionForMissingMerchantWithNotFound() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 1,
				"merchantId", 999,
				"categoryId", 5,
				"amount", "10.00",
				"direction", "DEBIT",
				"occurredOn", "2026-01-22",
				"description", "Unknown merchant"
		));

		mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNotFound())
				.andExpect(result -> assertThat(result.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void rejectsDuplicateTransactionWithConflict() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 1,
				"merchantId", 1,
				"categoryId", 5,
				"amount", "4.75",
				"direction", "DEBIT",
				"occurredOn", "2026-01-01",
				"description", "Morning coffee"
		));

		MvcResult result = mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isConflict())
				.andExpect(result2 -> assertThat(result2.getResponse().getContentType())
						.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$.status").value(409))
				.andReturn();

		OpenApiContractSupport.assertBodyMatchesSchema(
				"/transactions", "post", 409, "application/problem+json", result.getResponse().getContentAsString());
	}

	@Test
	void rejectsTransactionCreationWithoutToken() throws Exception {
		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void doesNotAllowOneUsersTokenToCreateTransactionsForAnotherAccount() throws Exception {
		String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
				"accountId", 1,
				"merchantId", 1,
				"categoryId", 5,
				"amount", "10.00",
				"direction", "DEBIT",
				"occurredOn", "2026-01-22",
				"description", "Ben tries Ada's account"
		));

		mockMvc.perform(post("/api/v1/transactions")
						.header(HttpHeaders.AUTHORIZATION, bearer(BEN_USER_ID))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isForbidden());
	}

	@Test
	void streamsDeterministicHardCodedTextInMultipleChunks() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post("/api/v1/insights/stream")
						.header(HttpHeaders.AUTHORIZATION, bearer(ADA_USER_ID)))
				.andExpect(request().asyncStarted())
				.andReturn();

		MvcResult result = mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body).contains("Reviewing this month's spending...");
		assertThat(body).contains("This is placeholder insight text. No model was called.");
		assertThat(body).doesNotContain("gpt").doesNotContain("claude").doesNotContain("anthropic");
		long eventCount = body.lines().filter(line -> line.startsWith("event:")).count();
		assertThat(eventCount).isGreaterThanOrEqualTo(2);

		OpenApiContractSupport.assertResponseDocumented("/insights/stream", "post", 200);
		OpenApiContractSupport.assertMediaTypeDocumented("/insights/stream", "post", 200, "text/event-stream");
	}

	@Test
	void rejectsInsightStreamWithoutToken() throws Exception {
		mockMvc.perform(post("/api/v1/insights/stream"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(long userId) {
		return "Bearer " + jwtTestSupport.accessTokenFor(userId);
	}

	private JsonNode readJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}
}
