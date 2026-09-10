package com.fedstack.spending.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "spending.auth.jwt")
public class JwtAuthProperties {
	private String issuer = "spending-dashboard-service";
	private String audience = "spending-dashboard-api";
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(7);

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = requireText(issuer, "issuer");
	}

	public String getAudience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = requireText(audience, "audience");
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = requirePositive(accessTokenTtl, "accessTokenTtl");
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = requirePositive(refreshTokenTtl, "refreshTokenTtl");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}
}
