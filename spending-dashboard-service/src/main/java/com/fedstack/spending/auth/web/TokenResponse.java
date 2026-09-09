package com.fedstack.spending.auth.web;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresInSeconds
) {
}
