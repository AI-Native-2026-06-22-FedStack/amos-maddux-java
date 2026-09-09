package com.fedstack.spending.auth.web;

import com.fedstack.spending.auth.application.AuthenticationService;
import com.fedstack.spending.auth.token.IssuedAccessToken;
import com.fedstack.spending.auth.token.IssuedTokenSet;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private static final String TOKEN_TYPE = "Bearer";

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = Objects.requireNonNull(
				authenticationService,
				"authenticationService must not be null"
		);
	}

	@PostMapping("/token")
	TokenResponse token(@RequestBody CredentialTokenRequest request) {
		IssuedTokenSet tokenSet = authenticationService.exchangeCredentials(request.email(), request.password());
		return new TokenResponse(
				tokenSet.accessToken(),
				tokenSet.refreshToken(),
				TOKEN_TYPE,
				tokenSet.accessTokenExpiresInSeconds()
		);
	}

	@PostMapping("/refresh")
	AccessTokenResponse refresh(@RequestBody RefreshTokenRequest request) {
		IssuedAccessToken accessToken = authenticationService.renewAccess(request.refreshToken());
		return new AccessTokenResponse(accessToken.accessToken(), TOKEN_TYPE, accessToken.expiresInSeconds());
	}

	@GetMapping("/caller")
	CallerResponse caller(@AuthenticationPrincipal Jwt jwt) {
		return new CallerResponse(jwt.getSubject());
	}
}
