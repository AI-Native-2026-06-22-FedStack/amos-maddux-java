# Spending Dashboard Decisions

## Why is `TransactionSource` the Lesson 5 boundary?

The application services need transactions for one user and one month, not knowledge of where those transactions live. `TransactionSource` captures that use case and lets Lesson 5 replace only the in-memory implementation with JPA while leaving the summary service, transaction-query service, and console behavior unchanged.

## Why does the source own filtering and ordering?

The source contract says monthly transactions are returned newest occurrence first, with descending identifiers only to break equal-occurrence ties. Keeping that ordering in the source means every caller sees the same deterministic result, and the transaction-list service can preserve the source order exactly as required.

## Why are summary and query separate services?

The dashboard has two visible use cases: monthly totals and the recent transaction list. `MonthlySummaryService` calculates income, spending, and net change, while `MonthlyTransactionQueryService` retrieves the source result without changing it. Keeping them separate makes each behavior easier to test without Spring.

## Why records and `BigDecimal`?

Transactions and summary results are values, so Java records keep them immutable and concise. Money uses `BigDecimal` to avoid binary floating-point rounding errors, and constructors require exactly two decimal places so invalid money cannot enter the application model silently.

## Why `YearMonth`?

The use cases operate on a requested month, not an arbitrary date range. `YearMonth` names that intent directly and avoids off-by-one date boundaries in callers.

## Why constructor injection only?

Each application service has one required collaborator: `TransactionSource`. Explicit constructors make those dependencies visible, easy to substitute in plain unit tests, and safe for Spring to wire without field injection, setter injection, or service lookup.

## Why keep authentication separate from spending services?

The Lesson 6 work establishes caller identity for future HTTP APIs without exposing spending data yet. New web, token, security, and credential-persistence code lives under `auth` and `security`, while the existing spending services still depend only on `TransactionSource`.

## Why asymmetric JWTs with startup-generated keys?

Access and refresh tokens are signed with an RSA key pair generated at application startup. That exercises asymmetric signing and verification without committing development private key material or implying a production key-rotation design.

## Why store only the current refresh-token hash?

Refresh tokens are bearer credentials, so the database stores only a SHA-256 digest of the current refresh JWT plus expiry/revocation state. A new sign-in replaces that digest, refresh renewal does not rotate it, and a stolen database row is not itself a reusable bearer token.

## Why a protected caller endpoint?

`GET /api/v1/auth/caller` is a narrow boundary test for Lesson 7. It returns only the verified JWT subject, creates no HTTP session, and does not expose dashboard, transaction, insight, or GraphQL behavior.
