package com.fedstack.spending.auth.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class JwtPurposeValidator implements OAuth2TokenValidator<Jwt> {
	private final TokenPurpose expectedPurpose;

	public JwtPurposeValidator(TokenPurpose expectedPurpose) {
		this.expectedPurpose = Objects.requireNonNull(expectedPurpose, "expectedPurpose must not be null");
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (expectedPurpose.claimValue().equals(token.getClaimAsString("purpose"))) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(
				"invalid_token",
				"JWT purpose is invalid",
				null
		));
	}
}
