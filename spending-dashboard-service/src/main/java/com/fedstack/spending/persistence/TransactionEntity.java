package com.fedstack.spending.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
		name = "transactions",
		indexes = @Index(name = "ix_transactions_account_occurred_on", columnList = "account_id, occurred_on DESC, id DESC")
)
public class TransactionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private AccountEntity account;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "merchant_id", nullable = false)
	private MerchantEntity merchant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoryEntity category;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "direction", nullable = false, length = 10)
	private String direction;

	@Column(name = "occurred_on", nullable = false)
	private LocalDate occurredOn;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected TransactionEntity() {
	}

	public TransactionEntity(
			AccountEntity account,
			MerchantEntity merchant,
			CategoryEntity category,
			BigDecimal amount,
			String direction,
			LocalDate occurredOn,
			String description,
			OffsetDateTime createdAt
	) {
		this.account = Objects.requireNonNull(account, "account must not be null");
		this.merchant = Objects.requireNonNull(merchant, "merchant must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.amount = Objects.requireNonNull(amount, "amount must not be null");
		this.direction = requireText(direction, "direction");
		this.occurredOn = Objects.requireNonNull(occurredOn, "occurredOn must not be null");
		this.description = requireText(description, "description");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public Long getId() {
		return id;
	}

	public AccountEntity getAccount() {
		return account;
	}

	public MerchantEntity getMerchant() {
		return merchant;
	}

	public CategoryEntity getCategory() {
		return category;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getDirection() {
		return direction;
	}

	public LocalDate getOccurredOn() {
		return occurredOn;
	}

	public String getDescription() {
		return description;
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
