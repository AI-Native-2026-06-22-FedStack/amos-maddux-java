# AI Usage Record

AI assistance was used to scaffold and implement this Spring Boot spending dashboard application-layer deliverable. The generated plan, project structure, domain records, transaction-source interface, in-memory source, constructor-injected application services, console runner, tests, and evidence notes were reviewed against the assignment requirements.

The implementation was checked for the Lesson 5 handoff boundary: both use cases depend on `TransactionSource`, the in-memory source owns filtering and deterministic ordering, and the transaction-query service preserves source order without sorting again. The code was also reviewed for BigDecimal money handling, immutable values, null/blank/identifier validation, and absence of controllers, JPA, JDBC, database configuration, field injection, setter injection, service lookup, and source construction inside services.

Verification was performed with the checked-in Gradle wrapper. Successful `./gradlew test` and `./gradlew bootRun` results are recorded in `evidence.md`.
