package com.fedstack.spending.application;

import com.fedstack.spending.persistence.AccountEntity;
import com.fedstack.spending.persistence.AccountJpaRepository;
import com.fedstack.spending.persistence.CategoryEntity;
import com.fedstack.spending.persistence.CategoryJpaRepository;
import com.fedstack.spending.persistence.MerchantEntity;
import com.fedstack.spending.persistence.MerchantJpaRepository;
import com.fedstack.spending.persistence.TransactionEntity;
import com.fedstack.spending.persistence.TransactionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class TransactionCreationService {
	private final TransactionJpaRepository transactionRepository;
	private final AccountJpaRepository accountRepository;
	private final MerchantJpaRepository merchantRepository;
	private final CategoryJpaRepository categoryRepository;
	private final Clock clock;

	public TransactionCreationService(
			TransactionJpaRepository transactionRepository,
			AccountJpaRepository accountRepository,
			MerchantJpaRepository merchantRepository,
			CategoryJpaRepository categoryRepository,
			Clock clock
	) {
		this.transactionRepository = Objects.requireNonNull(
				transactionRepository,
				"transactionRepository must not be null"
		);
		this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
		this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository must not be null");
		this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Transactional
	public CreatedTransaction createTransaction(long userId, CreateTransactionCommand command) {
		if (userId <= 0) {
			throw new IllegalArgumentException("user id must be positive");
		}
		Objects.requireNonNull(command, "command must not be null");

		AccountEntity account = accountRepository.findById(command.accountId())
				.orElseThrow(() -> new TransactionReferenceNotFoundException(
						"accountId " + command.accountId() + " does not exist"
				));
		if (!account.getUser().getId().equals(userId)) {
			throw new AccountNotOwnedException(
					"accountId " + command.accountId() + " is not owned by the authenticated user"
			);
		}
		MerchantEntity merchant = merchantRepository.findById(command.merchantId())
				.orElseThrow(() -> new TransactionReferenceNotFoundException(
						"merchantId " + command.merchantId() + " does not exist"
				));
		CategoryEntity category = categoryRepository.findById(command.categoryId())
				.orElseThrow(() -> new TransactionReferenceNotFoundException(
						"categoryId " + command.categoryId() + " does not exist"
				));

		boolean duplicate = transactionRepository.existsDuplicate(
				command.accountId(),
				command.merchantId(),
				command.categoryId(),
				command.amount(),
				command.direction(),
				command.occurredOn(),
				command.description()
		);
		if (duplicate) {
			throw new DuplicateTransactionException(
					"an identical transaction already exists for this account and date"
			);
		}

		TransactionEntity transaction = new TransactionEntity(
				account,
				merchant,
				category,
				command.amount(),
				command.direction(),
				command.occurredOn(),
				command.description(),
				OffsetDateTime.now(clock)
		);
		TransactionEntity saved = transactionRepository.save(transaction);
		return new CreatedTransaction(
				saved.getId(),
				account.getId(),
				merchant.getId(),
				category.getId(),
				saved.getAmount(),
				saved.getDirection(),
				saved.getOccurredOn(),
				saved.getDescription(),
				saved.getCreatedAt()
		);
	}
}
