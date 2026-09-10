package com.fedstack.spending.auth.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtRequiredClaimsValidator implements OAuth2TokenValidator<Jwt> {
	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (isBlank(token.getSubject()) || token.getIssuedAt() == null || token.getExpiresAt() == null) {
			return OAuth2TokenValidatorResult.failure(new OAuth2Error(
					"invalid_token",
					"JWT required claims are missing",
					null
			));
		}
		return OAuth2TokenValidatorResult.success();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
