package com.fedstack.spending.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "email", nullable = false, length = 255, unique = true)
	private String email;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected UserEntity() {
	}

	public UserEntity(String email, String displayName, OffsetDateTime createdAt) {
		this.email = requireText(email, "email");
		this.displayName = requireText(displayName, "displayName");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
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
