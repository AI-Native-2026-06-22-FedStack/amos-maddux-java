package com.fedstack.spending.auth.token;

public record IssuedAccessToken(String accessToken, long expiresInSeconds) {
}
