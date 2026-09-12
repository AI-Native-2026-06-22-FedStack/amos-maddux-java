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

## Why hand-write the OpenAPI contract instead of generating it from annotations?

The deliverable asks for the contract to be defined first, and a hand-written `openapi/openapi.yaml` stays the single source of truth for shape, examples, and error semantics rather than an artifact of whatever springdoc infers from controller annotations. No springdoc dependency was added; controllers were written to match the checked-in contract, and HTTP tests validate representative request/response bodies against it.

## Why validate the contract structurally instead of with a strict OpenAPI parser?

The contract declares `openapi: 3.2.0`. The Java OpenAPI-parsing libraries available on Maven Central at the time of this work (swagger-parser 2.1.22 and its swagger-request-validator consumers) only recognize `3.0.x`/`3.1.x` and silently fall back to a legacy Swagger 1.x parser for anything else, so they cannot validate a 3.2.0 document. Downgrading the declared version to fit older tooling was rejected as backwards — the contract should not be weakened to accommodate a library gap. Instead, `OpenApiContractSupport` loads `openapi.yaml` as plain YAML and validates captured response bodies against its `components.schemas` definitions with a version-agnostic JSON Schema validator (`com.networknt:json-schema-validator`). Spec-level 3.2.0 structural compliance (required attributes, resolvable `$ref`s) is verified separately by `npx @redocly/cli lint`, which does support 3.2.0.

## Why represent money as decimal strings in the JSON responses?

A `BigDecimal` serialized as a JSON number is still susceptible to precision loss for consumers that decode JSON numbers as floating point, and Jackson's default number serialization drops the two-decimal-place invariant the domain guarantees (`0.00` becomes `0.0`). The `Money` schema in the OpenAPI contract is `type: string` with a two-fraction-digit pattern; the web DTOs (`MonthlyDashboardResponse`, `DashboardTransactionResponse`, `CreateTransactionResponse`) format amounts with `BigDecimal.toPlainString()` so the wire representation is exact and matches the contract.

## Why does `TransactionCreationService` live in `application`, not a new `transaction` package?

It sits next to `MonthlySummaryService` and `MonthlyTransactionQueryService` as the third application-layer use case over the same `transactions` aggregate, and depends directly on `TransactionJpaRepository`, `AccountJpaRepository`, `MerchantJpaRepository`, and `CategoryJpaRepository` (new, minimal `JpaRepository` interfaces) rather than the read-only `TransactionSource` port, since creation needs identity-based lookups and a save that the read-side port was never designed for. Account ownership is checked against the verified token subject before persisting, independent of anything the client supplies.

## Why Problem Details only for the three new operations?

RFC 9457 Problem Details was introduced for `/api/v1/dashboard`, `/api/v1/transactions`, and `/api/v1/insights/stream` via a dedicated `ApiExceptionHandler` (`@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)`), scoped so it never intercepts exceptions thrown by the Lesson 6 auth endpoints. `AuthExceptionHandler` and its flat `{"error": "..."}` shape are unchanged, since migrating them was outside this deliverable's stated scope and would have touched passing Lesson 6 tests for no required behavior change. The one shared piece is the security filter chain's 401 entry point, which now emits `application/problem+json` for every protected endpoint (including the pre-existing `/api/v1/auth/caller`) so a missing or invalid bearer token has one consistent failure shape across the whole API surface.

## Why `SseEmitter` for the insight placeholder?

`POST /api/v1/insights/stream` only needs to prove the contract can carry a multi-chunk `text/event-stream` response; it is not a reactive endpoint and the rest of the application is Spring MVC, not WebFlux. `SseEmitter` sends deterministic, hard-coded chunks synchronously inside the handler and completes before returning — Spring MVC still dispatches it through the async request path, which is what a real future streaming insight would also need. No LLM SDK, API key, prompt, or `spending_insights` persistence was added; that table remains unmapped by any entity.
