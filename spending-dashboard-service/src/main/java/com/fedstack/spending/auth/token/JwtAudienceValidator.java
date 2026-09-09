package com.fedstack.spending.auth.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
	private final String expectedAudience;

	public JwtAudienceValidator(String expectedAudience) {
		this.expectedAudience = Objects.requireNonNull(expectedAudience, "expectedAudience must not be null");
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (token.getAudience().contains(expectedAudience)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(
				"invalid_token",
				"JWT audience is invalid",
				null
		));
	}
}
