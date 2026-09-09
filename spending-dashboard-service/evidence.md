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
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test

BUILD SUCCESSFUL in 31s
4 actionable tasks: 4 executed
```

## `./gradlew bootRun`

Command run with Java 21:

```text
JAVA_HOME=/tmp/codex-java21 GRADLE_USER_HOME=/tmp/codex-gradle-home PATH=/tmp/codex-java21/bin:$PATH ./gradlew bootRun
```

Result:

```text
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :resolveMainClassName UP-TO-DATE

> Task :bootRun
Monthly summary for user 1 in 2026-08
income:     5000.00
spending:   1726.00
net change: 3274.00
transaction IDs: 1004, 1003, 1002, 1001

BUILD SUCCESSFUL in 1s
4 actionable tasks: 2 executed, 2 up-to-date
```

## Spring context test evidence

`SpendingDashboardServiceApplicationTests` verifies that component scanning creates:

- `TransactionSource`
- `MonthlySummaryService`
- `MonthlyTransactionQueryService`
- `DashboardConsoleRunner`

## Plain unit-test evidence

- `MonthlySummaryServiceTest` verifies summary totals, source request boundaries, and empty monthly results without starting Spring.
- `MonthlyTransactionQueryServiceTest` verifies the query service preserves substitute source order without starting Spring.
- `InMemoryTransactionSourceTest` verifies user/month filtering, deterministic newest-first ordering, same-day descending ID tie-breaking, and immutable results.
- `TransactionTest` verifies defensive domain validation for trimmed descriptions, nonnegative money, and exact two-decimal money scale.
