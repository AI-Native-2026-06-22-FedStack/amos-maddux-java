package com.fedstack.spending.auth.application;

import com.fedstack.spending.auth.persistence.UserAuthCredentialsEntity;
import com.fedstack.spending.auth.persistence.UserAuthCredentialsRepository;
import com.fedstack.spending.auth.token.IssuedAccessToken;
import com.fedstack.spending.auth.token.IssuedTokenSet;
import com.fedstack.spending.auth.token.JwtTokenService;
import com.fedstack.spending.auth.token.RefreshTokenHasher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthenticationService {
	private final UserAuthCredentialsRepository credentialsRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;
	private final JwtDecoder refreshJwtDecoder;
	private final RefreshTokenHasher refreshTokenHasher;
	private final Clock clock;
	private final String unknownUserPasswordHash;

	public AuthenticationService(
			UserAuthCredentialsRepository credentialsRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService tokenService,
			@Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
			RefreshTokenHasher refreshTokenHasher,
			Clock clock
	) {
		this.credentialsRepository = Objects.requireNonNull(
				credentialsRepository,
				"credentialsRepository must not be null"
		);
		this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
		this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
		this.refreshJwtDecoder = Objects.requireNonNull(refreshJwtDecoder, "refreshJwtDecoder must not be null");
		this.refreshTokenHasher = Objects.requireNonNull(refreshTokenHasher, "refreshTokenHasher must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.unknownUserPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional
	public IssuedTokenSet exchangeCredentials(String email, String password) {
		String normalizedEmail = requireText(email);
		String rawPassword = requireText(password);

		UserAuthCredentialsEntity credentials = credentialsRepository.findByEmailForUpdate(normalizedEmail)
				.orElse(null);
		String storedPasswordHash = credentials == null ? unknownUserPasswordHash : credentials.getPasswordHash();
		if (!passwordEncoder.matches(rawPassword, storedPasswordHash) || credentials == null) {
			throw new AuthFailureException();
		}

		IssuedTokenSet tokenSet = tokenService.issueTokenSet(credentials.getUserId());
		credentials.replaceRefreshToken(
				refreshTokenHasher.hash(tokenSet.refreshToken()),
				tokenService.refreshTokenExpiresAt(),
				clock.instant()
		);
		return tokenSet;
	}

	@Transactional
	public IssuedAccessToken renewAccess(String refreshToken) {
		String rawRefreshToken = requireText(refreshToken);
		Jwt decodedRefreshToken = decodeRefreshToken(rawRefreshToken);
		long userId = parseSubject(decodedRefreshToken.getSubject());

		UserAuthCredentialsEntity credentials = credentialsRepository.findByUserIdForUpdate(userId)
				.orElseThrow(AuthFailureException::new);
		String presentedRefreshTokenHash = refreshTokenHasher.hash(rawRefreshToken);
		Instant now = clock.instant();

		if (!presentedRefreshTokenHash.equals(credentials.getCurrentRefreshTokenHash())
				|| credentials.getCurrentRefreshTokenExpiresAt() == null
				|| !credentials.getCurrentRefreshTokenExpiresAt().isAfter(now)
				|| credentials.getRefreshTokenRevokedAt() != null) {
			throw new AuthFailureException();
		}

		return tokenService.issueAccessToken(userId);
	}

	private Jwt decodeRefreshToken(String refreshToken) {
		try {
			return refreshJwtDecoder.decode(refreshToken);
		} catch (JwtException exception) {
			throw new AuthFailureException();
		}
	}

	private static long parseSubject(String subject) {
		try {
			long userId = Long.parseLong(subject);
			if (userId <= 0) {
				throw new AuthFailureException();
			}
			return userId;
		} catch (NumberFormatException exception) {
			throw new AuthFailureException();
		}
	}

	private static String requireText(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new AuthFailureException();
		}
		return value.trim();
	}
}
