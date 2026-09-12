package com.fedstack.spending.support;

import com.fedstack.spending.auth.token.JwtAuthProperties;
import com.fedstack.spending.auth.token.TokenPurpose;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Mints signed access tokens for HTTP tests without exercising the sign-in
 * endpoint, mirroring the request/response shape {@code JwtTokenService}
 * issues in production.
 */
public final class JwtTestSupport {
	private final JwtAuthProperties jwtProperties;
	private final JwtEncoder jwtEncoder;

	public JwtTestSupport(JwtAuthProperties jwtProperties, JwtEncoder jwtEncoder) {
		this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties must not be null");
		this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder must not be null");
	}

	public String accessTokenFor(long userId) {
		Instant now = Instant.now();
		return signedToken(
				String.valueOf(userId),
				jwtProperties.getIssuer(),
				jwtProperties.getAudience(),
				TokenPurpose.ACCESS,
				now,
				now.plus(jwtProperties.getAccessTokenTtl()),
				jwtEncoder
		);
	}

	public String signedToken(
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
}
