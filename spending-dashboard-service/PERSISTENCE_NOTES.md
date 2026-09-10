# Persistence Notes

## Relationship Fetching

The required to-one relationships from `spending_dashboard_schema.sql` are mapped as lazy JPA associations:

- `AccountEntity.user` maps `accounts.user_id -> users.id` with `@ManyToOne(fetch = FetchType.LAZY, optional = false)`.
- `TransactionEntity.account` maps `transactions.account_id -> accounts.id` with `@ManyToOne(fetch = FetchType.LAZY, optional = false)`.
- `TransactionEntity.merchant` maps `transactions.merchant_id -> merchants.id` with `@ManyToOne(fetch = FetchType.LAZY, optional = false)`.
- `TransactionEntity.category` maps `transactions.category_id -> categories.id` with `@ManyToOne(fetch = FetchType.LAZY, optional = false)`.

Lazy mappings keep the default entity model conservative. A transaction does not always need its account owner, merchant, and category, so making every to-one association globally eager would bake this dashboard read's needs into every persistence use of the entity. That would make future reads pay for joins even when they only need transaction scalar fields.

If the monthly dashboard query loaded only `TransactionEntity` rows and the application then touched `transaction.getAccount()`, `transaction.getAccount().getUser()`, `transaction.getMerchant()`, or `transaction.getCategory()` for each result, Hibernate could issue additional lazy-loading SQL per transaction or per distinct referenced row. That is the N+1 path this read needs to avoid.

## Monthly Fetch Plan

The monthly dashboard repository method uses JPQL fetch joins for exactly the to-one associations needed by this read:

```java
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
```

The `transaction.account` and `account.user` joins enforce user ownership through the relational path defined by the schema: `transactions.account_id -> accounts.id`, then `accounts.user_id -> users.id`. The adapter also uses this path to populate the existing application `Transaction.userId`.

The `transaction.merchant` and `transaction.category` joins load the related values the product transaction display expects to be available for a dashboard transaction. The current application transaction value only exposes id, user id, occurred date, type, amount, and description, but the persistence read is shaped so merchant and category are already available at the boundary if the read model grows to match the product display.

These query-level fetch joins address the per-result lazy-loading risk for the monthly dashboard read without changing the entity defaults. The mappings stay lazy because fetch policy belongs to the general entity model, while this query's fetch plan belongs to this specific use case.

The repository tests keep SQL logging enabled and show Hibernate executing the monthly read as a joined query. A useful excerpt for evidence is:

```text
select ... from transactions te1_0
join accounts a1_0 on a1_0.id=te1_0.account_id
join users u1_0 on u1_0.id=a1_0.user_id
join merchants m1_0 on m1_0.id=te1_0.merchant_id
join categories c1_0 on c1_0.id=te1_0.category_id
where u1_0.id=? and te1_0.occurred_on>=? and te1_0.occurred_on<?
order by te1_0.occurred_on desc,te1_0.id desc
```

The tests also assert that `account`, `account.user`, `merchant`, and `category` are initialized after the monthly query.

## Persistence Boundary

Open Session in View is disabled with `spring.jpa.open-in-view=false`. That keeps lazy loading out of presentation or application-boundary code and makes repository/adapter fetch plans explicit.

`JpaTransactionSource` is the persistence adapter for the existing `TransactionSource` interface. Its `findMonthlyTransactions(long userId, YearMonth month)` method is read-only transactional, calls the monthly repository method with a half-open month range, and converts each `TransactionEntity` into the existing application `Transaction` record before returning.

That conversion maps:

- `TransactionEntity.id` to `Transaction.id`
- `TransactionEntity.account.user.id` to `Transaction.userId`
- `TransactionEntity.occurredOn` to `Transaction.occurredOn`
- `TransactionEntity.direction` values `CREDIT` and `DEBIT` to `TransactionType.INCOME` and `TransactionType.EXPENSE`
- `TransactionEntity.amount` to `Transaction.amount`
- `TransactionEntity.description` to `Transaction.description`

Because the adapter returns application values instead of entities, lazy references do not escape into the Spring Boot application services or console runner. The services still depend only on `TransactionSource`, and persistence behavior stays inside `com.fedstack.spending.persistence`.

The main tradeoff in this specific to-one fetch plan is that every monthly dashboard transaction row includes account, user, merchant, and category columns even if the current application value does not expose merchant and category yet. That is acceptable here because the read is dashboard-specific, the associations are all required to-one references in the schema, and the product display needs those related values. The broader entity model remains lazy for other reads.
