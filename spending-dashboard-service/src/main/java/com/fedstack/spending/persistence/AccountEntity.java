package com.fedstack.spending.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
		name = "accounts",
		uniqueConstraints = @UniqueConstraint(name = "uq_accounts_user_name", columnNames = {"user_id", "name"})
)
public class AccountEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "account_type", nullable = false, length = 20)
	private String accountType;

	@Column(name = "opened_on", nullable = false)
	private LocalDate openedOn;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected AccountEntity() {
	}

	public AccountEntity(
			UserEntity user,
			String name,
			String accountType,
			LocalDate openedOn,
			OffsetDateTime createdAt
	) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.name = requireText(name, "name");
		this.accountType = requireText(accountType, "accountType");
		this.openedOn = Objects.requireNonNull(openedOn, "openedOn must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public Long getId() {
		return id;
	}

	public UserEntity getUser() {
		return user;
	}

	public String getName() {
		return name;
	}

	public String getAccountType() {
		return accountType;
	}

	public LocalDate getOpenedOn() {
		return openedOn;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	private static String requireText(String value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return trimmed;
	}
}
