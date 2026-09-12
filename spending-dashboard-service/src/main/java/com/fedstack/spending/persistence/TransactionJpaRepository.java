package com.fedstack.spending.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {
	@Query("""
			select transaction
			from TransactionEntity transaction
			join fetch transaction.account account
			join fetch account.user user
			join fetch transaction.merchant merchant
			join fetch transaction.category category
			where user.id = :userId
				and transaction.occurredOn >= :start
				and transaction.occurredOn < :end
			order by transaction.occurredOn desc, transaction.id desc
			""")
	List<TransactionEntity> findMonthlyDashboardTransactions(
			@Param("userId") Long userId,
			@Param("start") LocalDate start,
			@Param("end") LocalDate end
	);

	@Query("""
			select count(transaction) > 0
			from TransactionEntity transaction
			where transaction.account.id = :accountId
				and transaction.merchant.id = :merchantId
				and transaction.category.id = :categoryId
				and transaction.amount = :amount
				and transaction.direction = :direction
				and transaction.occurredOn = :occurredOn
				and transaction.description = :description
			""")
	boolean existsDuplicate(
			@Param("accountId") Long accountId,
			@Param("merchantId") Long merchantId,
			@Param("categoryId") Long categoryId,
			@Param("amount") BigDecimal amount,
			@Param("direction") String direction,
			@Param("occurredOn") LocalDate occurredOn,
			@Param("description") String description
	);
}
