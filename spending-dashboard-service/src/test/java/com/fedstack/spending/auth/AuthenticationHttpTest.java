package com.fedstack.spending.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedstack.spending.auth.token.JwtAuthProperties;
import com.fedstack.spending.auth.token.RefreshTokenHasher;
import com.fedstack.spending.auth.token.TokenPurpose;
import com.fedstack.spending.support.PostgreSqlContainerSupport;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthenticationHttpTest extends PostgreSqlContainerSupport {
	private static final long ADA_USER_ID = 1L;
	private static final String ADA_EMAIL = "ada@example.com";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	private final JwtDecoder accessJwtDecoder;
	private final JwtDecoder refreshJwtDecoder;
	private final JwtEncoder jwtEncoder;
	private final JwtAuthProperties jwtProperties;
	private final RefreshTokenHasher refreshTokenHasher;

	private String validPassword;

	@Autowired
	AuthenticationHttpTest(
			MockMvc mockMvc,
			ObjectMapper objectMapper,
			JdbcTemplate jdbcTemplate,
			PasswordEncoder passwordEncoder,
			JwtDecoder accessJwtDecoder,
			@Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
			JwtEncoder jwtEncoder,
			JwtAuthProperties jwtProperties,
			RefreshTokenHasher refreshTokenHasher
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
		this.accessJwtDecoder = accessJwtDecoder;
		this.refreshJwtDecoder = refreshJwtDecoder;
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
		this.refreshTokenHasher = refreshTokenHasher;
	}

	@BeforeEach
	void seedRuntimeCredentials() {
		validPassword = newSecret();
		jdbcTemplate.update("delete from user_auth_credentials");
		jdbcTemplate.update(
				"insert into user_auth_credentials (user_id, password_hash) values (?, ?)",
				ADA_USER_ID,
				passwordEncoder.encode(validPassword)
		);
	}

	@Test
	void exchangesValidCredentialsForDistinctAccessAndRefreshTokens() throws Exception {
		JsonNode response = signIn(ADA_EMAIL, validPassword);

		assertThat(response.path("tokenType").asText()).isEqualTo("Bearer");
		assertThat(response.path("expiresInSeconds").asLong()).isEqualTo(900);

		String accessToken = response.path("accessToken").asText();
		String refreshToken = response.path("refreshToken").asText();
		assertThat(accessToken).isNotBlank();
		assertThat(refreshToken).isNotBlank();
		assertThat(accessToken).isNotEqualTo(refreshToken);

		Jwt access = accessJwtDecoder.decode(accessToken);
		Jwt refresh = refreshJwtDecoder.decode(refreshToken);

		assertRequiredClaims(access, TokenPurpose.ACCESS);
		assertRequiredClaims(refresh, TokenPurpose.REFRESH);
		assertThat(access.getExpiresAt()).isBefore(refresh.getExpiresAt());
		assertThat(currentRefreshTokenHash()).isEqualTo(refreshTokenHasher.hash(refreshToken));
	}

	@Test
	void rejectsUnknownUsersAndWrongPasswordsTheSameWay() throws Exception {
		MvcResult unknownUser = tokenRequest("unknown-" + UUID.randomUUID() + "@example.com", validPassword)
				.andExpect(status().isUnauthorized())
				.andReturn();
		MvcResult wrongPassword = tokenRequest(ADA_EMAIL, newSecret())
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(unknownUser.getResponse().getContentAsString())
				.isEqualTo(wrongPassword.getResponse().getContentAsString())
				.contains("invalid_credentials");
	}

	@Test
	void renewsAccessWithCurrentRefreshTokenWithoutRotatingIt() throws Exception {
		JsonNode signInResponse = signIn(ADA_EMAIL, validPassword);
		String refreshToken = signInResponse.path("refreshToken").asText();
		String originalRefreshTokenHash = currentRefreshTokenHash();

		JsonNode refreshResponse = refresh(refreshToken);

		assertThat(refreshResponse.path("accessToken").asText()).isNotBlank();
		assertThat(refreshResponse.has("refreshToken")).isFalse();
		assertThat(refreshResponse.path("accessToken").asText()).isNotEqualTo(signInResponse.path("accessToken").asText());
		assertThat(currentRefreshTokenHash()).isEqualTo(originalRefreshTokenHash);

		Jwt renewedAccess = accessJwtDecoder.decode(refreshResponse.path("accessToken").asText());
		assertRequiredClaims(renewedAccess, TokenPurpose.ACCESS);
	}

	@Test
	void rejectsInvalidExpiredReplacedAndRevokedRefreshTokens() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", "not-a-refresh-token"))))
				.andExpect(status().isUnauthorized());

		Instant now = Instant.now();
		String expiredRefreshToken = signedToken(
				String.valueOf(ADA_USER_ID),
				jwtProperties.getIssuer(),
				jwtProperties.getAudience(),
				TokenPurpose.REFRESH,
				now.minusSeconds(120),
				now.minusSeconds(60),
				jwtEncoder
		);
		jdbcTemplate.update(
				"update user_auth_credentials "
						+ "set current_refresh_token_hash = ?, "
						+ "current_refresh_token_expires_at = now() + interval '1 hour', "
						+ "refresh_token_revoked_at = null "
						+ "where user_id = ?",
				refreshTokenHasher.hash(expiredRefreshToken),
				ADA_USER_ID
		);
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", expiredRefreshToken))))
				.andExpect(status().isUnauthorized());

		JsonNode firstSignIn = signIn(ADA_EMAIL, validPassword);
		String currentRefreshToken = firstSignIn.path("refreshToken").asText();
		jdbcTemplate.update(
				"update user_auth_credentials set current_refresh_token_expires_at = now() - interval '60 seconds' "
						+ "where user_id = ?",
				ADA_USER_ID
		);
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", currentRefreshToken))))
				.andExpect(status().isUnauthorized());

		JsonNode replacedSignIn = signIn(ADA_EMAIL, validPassword);
		String replacedRefreshToken = replacedSignIn.path("refreshToken").asText();
		signIn(ADA_EMAIL, validPassword);
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", replacedRefreshToken))))
				.andExpect(status().isUnauthorized());

		JsonNode revokedSignIn = signIn(ADA_EMAIL, validPassword);
		String revokedRefreshToken = revokedSignIn.path("refreshToken").asText();
		jdbcTemplate.update(
				"update user_auth_credentials set refresh_token_revoked_at = now() where user_id = ?",
				ADA_USER_ID
		);
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", revokedRefreshToken))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void acceptsValidAccessTokenForProtectedCallerWithoutCreatingSession() throws Exception {
		String accessToken = signIn(ADA_EMAIL, validPassword).path("accessToken").asText();

		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value(String.valueOf(ADA_USER_ID)))
				.andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
	}

	@Test
	void rejectsInvalidAccessTokenCasesForProtectedCaller() throws Exception {
		mockMvc.perform(get("/api/v1/auth/caller"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());

		Instant now = Instant.now();
		String expiredAccess = signedToken(
				String.valueOf(ADA_USER_ID),
				jwtProperties.getIssuer(),
				jwtProperties.getAudience(),
				TokenPurpose.ACCESS,
				now.minusSeconds(120),
				now.minusSeconds(60),
				jwtEncoder
		);
		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccess))
				.andExpect(status().isUnauthorized());

		String wrongIssuer = signedToken(
				String.valueOf(ADA_USER_ID),
				"wrong-issuer",
				jwtProperties.getAudience(),
				TokenPurpose.ACCESS,
				now,
				now.plusSeconds(300),
				jwtEncoder
		);
		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongIssuer))
				.andExpect(status().isUnauthorized());

		String wrongAudience = signedToken(
				String.valueOf(ADA_USER_ID),
				jwtProperties.getIssuer(),
				"wrong-audience",
				TokenPurpose.ACCESS,
				now,
				now.plusSeconds(300),
				jwtEncoder
		);
		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongAudience))
				.andExpect(status().isUnauthorized());

		String wrongSignature = signedToken(
				String.valueOf(ADA_USER_ID),
				jwtProperties.getIssuer(),
				jwtProperties.getAudience(),
				TokenPurpose.ACCESS,
				now,
				now.plusSeconds(300),
				differentJwtEncoder()
		);
		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSignature))
				.andExpect(status().isUnauthorized());

		String refreshToken = signIn(ADA_EMAIL, validPassword).path("refreshToken").asText();
		mockMvc.perform(get("/api/v1/auth/caller")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
				.andExpect(status().isUnauthorized());
	}

	private org.springframework.test.web.servlet.ResultActions tokenRequest(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"email", email,
						"password", password
				))));
	}

	private JsonNode signIn(String email, String password) throws Exception {
		MvcResult result = tokenRequest(email, password)
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private JsonNode refresh(String refreshToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private void assertRequiredClaims(Jwt jwt, TokenPurpose purpose) {
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(jwtProperties.getIssuer());
		assertThat(jwt.getAudience()).contains(jwtProperties.getAudience());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(ADA_USER_ID));
		assertThat(jwt.getIssuedAt()).isNotNull();
		assertThat(jwt.getExpiresAt()).isNotNull();
		assertThat(jwt.getClaimAsString("purpose")).isEqualTo(purpose.claimValue());
	}

	private String currentRefreshTokenHash() {
		return jdbcTemplate.queryForObject(
				"select current_refresh_token_hash from user_auth_credentials where user_id = ?",
				String.class,
				ADA_USER_ID
		);
	}

	private String signedToken(
			String subject,
			String issuer,
			String audience,
			TokenPurpose purpose,
			Instant issuedAt,
			Instant expiresAt,
			JwtEncoder encoder
	) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.audience(List.of(audience))
				.subject(subject)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("purpose", purpose.claimValue())
				.build();
		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
	}

	private static JwtEncoder differentJwtEncoder() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
				.privateKey((RSAPrivateKey) keyPair.getPrivate())
				.keyID(UUID.randomUUID().toString())
				.algorithm(JWSAlgorithm.RS256)
				.build();
		return new NimbusJwtEncoder((jwkSelector, context) -> jwkSelector.select(new JWKSet(rsaKey)));
	}

	private static String newSecret() {
		return UUID.randomUUID().toString();
	}
}
