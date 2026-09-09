package com.fedstack.spending.auth.token;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtTokenService {
	private final JwtEncoder jwtEncoder;
	private final JwtAuthProperties properties;
	private final Clock clock;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtAuthProperties properties, Clock clock) {
		this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder must not be null");
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public IssuedTokenSet issueTokenSet(long userId) {
		String accessToken = issueToken(userId, TokenPurpose.ACCESS, properties.getAccessTokenTtl()).getTokenValue();
		String refreshToken = issueToken(userId, TokenPurpose.REFRESH, properties.getRefreshTokenTtl()).getTokenValue();
		return new IssuedTokenSet(
				accessToken,
				refreshToken,
				properties.getAccessTokenTtl().toSeconds()
		);
	}

	public IssuedAccessToken issueAccessToken(long userId) {
		String accessToken = issueToken(userId, TokenPurpose.ACCESS, properties.getAccessTokenTtl()).getTokenValue();
		return new IssuedAccessToken(accessToken, properties.getAccessTokenTtl().toSeconds());
	}

	public Instant refreshTokenExpiresAt() {
		return clock.instant().plus(properties.getRefreshTokenTtl());
	}

	private Jwt issueToken(long userId, TokenPurpose purpose, java.time.Duration ttl) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getIssuer())
				.audience(List.of(properties.getAudience()))
				.subject(String.valueOf(userId))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(ttl))
				.id(UUID.randomUUID().toString())
				.claim("purpose", purpose.claimValue())
				.build();
		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));
	}
}
