package com.fedstack.spending.auth.persistence;

import com.fedstack.spending.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_auth_credentials")
public class UserAuthCredentialsEntity {
	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "current_refresh_token_hash", length = 64)
	private String currentRefreshTokenHash;

	@Column(name = "current_refresh_token_expires_at")
	private Instant currentRefreshTokenExpiresAt;

	@Column(name = "refresh_token_revoked_at")
	private Instant refreshTokenRevokedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserAuthCredentialsEntity() {
	}

	public UserAuthCredentialsEntity(UserEntity user, String passwordHash, Instant now) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.passwordHash = requireText(passwordHash, "passwordHash");
		this.createdAt = Objects.requireNonNull(now, "now must not be null");
		this.updatedAt = now;
	}

	public Long getUserId() {
		return userId;
	}

	public UserEntity getUser() {
		return user;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getCurrentRefreshTokenHash() {
		return currentRefreshTokenHash;
	}

	public Instant getCurrentRefreshTokenExpiresAt() {
		return currentRefreshTokenExpiresAt;
	}

	public Instant getRefreshTokenRevokedAt() {
		return refreshTokenRevokedAt;
	}

	public void replaceRefreshToken(String refreshTokenHash, Instant expiresAt, Instant now) {
		this.currentRefreshTokenHash = requireHexHash(refreshTokenHash);
		this.currentRefreshTokenExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.refreshTokenRevokedAt = null;
		this.updatedAt = Objects.requireNonNull(now, "now must not be null");
	}

	public void revokeRefreshToken(Instant now) {
		this.refreshTokenRevokedAt = Objects.requireNonNull(now, "now must not be null");
		this.updatedAt = now;
	}

	private static String requireText(String value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return trimmed;
	}

	private static String requireHexHash(String value) {
		String trimmed = requireText(value, "refreshTokenHash");
		if (!trimmed.matches("[0-9a-f]{64}")) {
			throw new IllegalArgumentException("refresh token hash must be a SHA-256 hex digest");
		}
		return trimmed;
	}
}
