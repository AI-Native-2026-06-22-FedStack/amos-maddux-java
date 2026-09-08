# Spending Dashboard Evidence

## `./gradlew test`

Command run with Java 21:

```text
JAVA_HOME=/tmp/codex-java21 GRADLE_USER_HOME=/tmp/codex-gradle-home PATH=/tmp/codex-java21/bin:$PATH ./gradlew test
```

Result:

```text
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test

BUILD SUCCESSFUL in 10s
5 actionable tasks: 1 executed, 4 up-to-date
```

## Final JPQL Repository Method

`TransactionJpaRepository.findMonthlyDashboardTransactions(...)`:

```java
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
```

The method uses JPQL entity and property names, not native SQL.

## Seeded Ada January Read

The deterministic seed data defines Ada as user id `1`. For:

```text
userId = 1
start = 2026-01-01
end = 2026-02-01
```

the repository test verifies the returned transaction ids are exactly:

```text
4, 5, 3, 2, 1
```

Those rows are:

```text
id 4  2026-01-31  Acme Payroll        Income      2400.00  CREDIT
id 5  2026-01-20  City Power & Light  Utilities     96.40  DEBIT
id 3  2026-01-15  Metro Transit       Transport      2.75  DEBIT
id 2  2026-01-15  Whole Foods Market  Groceries     82.30  DEBIT
id 1  2026-01-01  Blue Bottle Coffee  Dining         4.75  DEBIT
```

The test confirms:

- transaction `1` is included because `occurred_on = 2026-01-01` matches the inclusive start.
- transaction `6` is excluded because `occurred_on = 2026-02-01` matches the exclusive end.
- transaction `7` is excluded because `occurred_on = 2025-12-31` is before the start.
- Ben's transactions `8` and `9` are excluded because the query scopes through `transaction.account.user`.
- transactions `2` and `3` share `2026-01-15`, and `3` appears before `2` because `id DESC` is the tie-breaker.

## Repository Test Coverage

`TransactionJpaRepositoryTest` verifies:

- Hibernate starts with `spring.jpa.hibernate.ddl-auto=validate`.
- Testcontainers PostgreSQL uses the supplied schema and seed scripts.
- the seed loaded `3` users and `9` transactions.
- Ada's January result is user-scoped and half-open by date.
- Cleo, user id `3`, receives an empty monthly result.
- account, account owner, merchant, and category references are initialized by the monthly JPQL fetch plan.
- a new user/account/merchant/category/transaction graph round-trips through PostgreSQL after `flush()` and `clear()`.

There is no exact Hibernate prepared-statement-count assertion.

## SQL Log Excerpt

The repository test logs include this monthly JPQL query rendered by Hibernate:

```text
select te1_0.id,te1_0.account_id,a1_0.id,a1_0.account_type,a1_0.created_at,a1_0.name,a1_0.opened_on,a1_0.user_id,u1_0.id,u1_0.created_at,u1_0.display_name,u1_0.email,te1_0.amount,te1_0.category_id,c1_0.id,c1_0.created_at,c1_0.name,te1_0.created_at,te1_0.description,te1_0.direction,te1_0.merchant_id,m1_0.id,m1_0.created_at,m1_0.name,te1_0.occurred_on
from transactions te1_0
join accounts a1_0 on a1_0.id=te1_0.account_id
join users u1_0 on u1_0.id=a1_0.user_id
join merchants m1_0 on m1_0.id=te1_0.merchant_id
join categories c1_0 on c1_0.id=te1_0.category_id
where u1_0.id=? and te1_0.occurred_on>=? and te1_0.occurred_on<?
order by te1_0.occurred_on desc,te1_0.id desc
```

## Application Boundary

The existing Spring Boot application services and console runner remain unchanged:

- `MonthlySummaryService`
- `MonthlyTransactionQueryService`
- `DashboardConsoleRunner`

They still depend on the original `TransactionSource` boundary. The production implementation is `JpaTransactionSource`; `InMemoryTransactionSource` is available only under the `in-memory` profile.

## Persistence Notes

See `PERSISTENCE_NOTES.md` for the lazy relationship boundaries, possible N+1 path, JPQL fetch plan, disabled Open Session in View setting, and entity-to-application mapping rationale.
