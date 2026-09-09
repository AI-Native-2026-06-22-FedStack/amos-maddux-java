package com.fedstack.spending.auth.token;

public record IssuedTokenSet(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
}
