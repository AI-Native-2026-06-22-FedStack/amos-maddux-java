package com.fedstack.spending.auth.web;

public record AccessTokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
