# Spending Dashboard Evidence

## `./gradlew test`

Command run with Java 21:

```text
JAVA_HOME=/tmp/codex-java-tools/jdk-21.0.12.1+1 GRADLE_USER_HOME=/tmp/codex-gradle-home PATH=/tmp/codex-java-tools/jdk-21.0.12.1+1/bin:$PATH ./gradlew test
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

BUILD SUCCESSFUL in 15s
5 actionable tasks: 1 executed, 4 up-to-date
```

The test suite covers the existing spending domain/application/persistence tests plus the new HTTP authentication boundary tests.

## Authentication Boundary Evidence

`AuthenticationHttpTest` verifies the web application context, MockMvc security filters, Testcontainers PostgreSQL schema, and the authentication lifecycle through HTTP:

- `POST /api/v1/auth/token` accepts runtime-generated synthetic credentials for seeded user id `1`.
- the token response contains a Bearer access JWT and refresh JWT; the decoded non-sensitive claims include issuer `spending-dashboard-service`, audience `spending-dashboard-api`, subject `1`, `iat`, `exp`, and distinct `purpose` values of `access` and `refresh`.
- the access token expires before the refresh token.
- unknown email and wrong password both return `401` with the same generic `invalid_credentials` response.
- sign-in stores only the SHA-256 digest of the current refresh token.
- `POST /api/v1/auth/refresh` returns a new access token and does not rotate the saved refresh-token digest.
- malformed, JWT-expired, saved-state-expired, replaced, and revoked refresh tokens are rejected.
- `GET /api/v1/auth/caller` returns only the verified subject for a valid access token and does not create an HTTP session.
- missing, malformed, expired, wrongly signed, wrong-issuer, wrong-audience, and refresh-purpose tokens are rejected for protected access.

No full token, token signature, plaintext password, password hash, refresh-token digest, or private key is recorded here.

## Current Source Organization

```text
com/fedstack/spending
com/fedstack/spending/application
com/fedstack/spending/auth/application
com/fedstack/spending/auth/persistence
com/fedstack/spending/auth/token
com/fedstack/spending/auth/web
com/fedstack/spending/console
com/fedstack/spending/domain
com/fedstack/spending/persistence
com/fedstack/spending/security
com/fedstack/spending/source
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

## Lesson 7 API Verification (2026-09-11)

This section records the Lesson 7 checks rather than referring to them only from
`AGENTS.md`. Synthetic JWTs are generated by the tests; no credential or token
value is included here.

### OpenAPI lint

```text
$ NPM_CONFIG_CACHE=/tmp/codex-redocly-cache npx --yes @redocly/cli@latest lint openapi/openapi.yaml
validating openapi/openapi.yaml...
openapi/openapi.yaml: validated in 61ms

Woohoo! Your API description is valid.
```

### Authenticated API exercises

`TransactionApiHttpTest` performs these MockMvc requests against the
Testcontainers PostgreSQL fixture and validates the associated OpenAPI response
schema where applicable:

- An authenticated `GET /api/v1/dashboard?month=2026-01` for Ada (subject
  `1`) returns `200`, month `2026-01`, income `2400.00`, spending `186.20`, net
  change `2213.80`, and transaction ids `4, 5, 3, 2, 1` in that order.
- An authenticated `POST /api/v1/transactions` with account `1`, merchant `1`,
  category `5`, amount `"12.34"`, direction `"DEBIT"`, date `2026-01-22`, and
  description `"Afternoon coffee"` returns `201`, a `Location` matching
  `/api/v1/transactions/{id}`, and decimal-safe string fields in the response.
- The same endpoint with Ben's subject (`2`) and Ada's account (`1`) returns
  `403`; Ada's dashboard response never contains Ben's seeded transaction ids
  `8` or `9`.

### SSE capture

The authenticated insight-stream test verifies `200 text/event-stream` and at
least two events. The deterministic stream emits these chunks:

```text
event:insight
data:Reviewing this month's spending...

event:insight
data:This is placeholder insight text. No model was called.

event:done
data:complete
```

The test also verifies the stream body has no `gpt`, `claude`, or `anthropic`
text, and that the OpenAPI operation documents `text/event-stream`.

### Problem Details examples

The HTTP tests assert `application/problem+json` and validate these response
types against the contract:

- malformed dashboard month: `400`, title `Malformed Request`;
- invalid transaction amount: `400`, title `Validation Failed`;
- missing merchant: `404`, title `Not Found`;
- duplicate transaction: `409`, title `Conflict`;
- unauthenticated dashboard or transaction request: `401`.

### Web/persistence boundary check

`TransactionController` imports only `CreatedTransaction` from the creation
use case. `TransactionCreationService` performs the entity mapping within its
transaction and returns that plain application record. Consequently the web
controller neither imports nor invokes `TransactionEntity`, `getAccount()`,
`getMerchant()`, or `getCategory()`.
