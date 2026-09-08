-- Personal Spending Dashboard — deterministic seed data.
--
-- Run after spending_dashboard_schema.sql. Every key is explicit so the
-- expected results below are stable across machines.
--
-- The data is shaped to make the required behaviours observable:
--   * user isolation      — Ada (1) and Ben (2) both have January transactions
--   * empty state         — Cleo (3) owns an account with no transactions
--   * half-open month     — 2026-01-01 is included, 2026-02-01 is excluded
--   * ordering tie-break  — transactions 2 and 3 share 2026-01-15
--   * fetched references  — every transaction has a merchant and a category
--
-- Expected read: user 1, month [2026-01-01, 2026-02-01), newest occurrence
-- first with descending id breaking equal-occurrence ties:
--
--   id 4  2026-01-31  Acme Payroll        Income      2400.00  CREDIT
--   id 5  2026-01-20  City Power & Light  Utilities     96.40  DEBIT
--   id 3  2026-01-15  Metro Transit       Transport      2.75  DEBIT
--   id 2  2026-01-15  Whole Foods Market  Groceries     82.30  DEBIT
--   id 1  2026-01-01  Blue Bottle Coffee  Dining         4.75  DEBIT

BEGIN;

TRUNCATE spending_insights, transactions, categories, merchants, accounts, users RESTART IDENTITY CASCADE;

INSERT INTO users (id, email, display_name) VALUES
    (1, 'ada@example.com',  'Ada Lovelace'),
    (2, 'ben@example.com',  'Ben Carter'),
    (3, 'cleo@example.com', 'Cleo Nakamura');

INSERT INTO accounts (id, user_id, name, account_type, opened_on) VALUES
    (1, 1, 'Everyday Checking', 'CHECKING',    DATE '2024-03-04'),
    (2, 1, 'Travel Card',       'CREDIT_CARD', DATE '2025-06-18'),
    (3, 2, 'Everyday Checking', 'CHECKING',    DATE '2024-11-22'),
    (4, 3, 'Everyday Checking', 'CHECKING',    DATE '2025-09-30');

INSERT INTO merchants (id, name) VALUES
    (1, 'Blue Bottle Coffee'),
    (2, 'Metro Transit'),
    (3, 'Whole Foods Market'),
    (4, 'Acme Payroll'),
    (5, 'City Power & Light');

INSERT INTO categories (id, name, parent_category_id) VALUES
    (1, 'Living',    NULL),
    (2, 'Groceries', 1),
    (3, 'Transport', 1),
    (4, 'Utilities', 1),
    (5, 'Dining',    1),
    (6, 'Income',    NULL);

INSERT INTO transactions (id, account_id, merchant_id, category_id, amount, direction, occurred_on, description) VALUES
    -- Ada — inside the January 2026 window
    (1, 1, 1, 5,    4.75, 'DEBIT',  DATE '2026-01-01', 'Morning coffee'),
    (2, 1, 3, 2,   82.30, 'DEBIT',  DATE '2026-01-15', 'Weekly groceries'),
    (3, 1, 2, 3,    2.75, 'DEBIT',  DATE '2026-01-15', 'Metro fare'),
    (4, 1, 4, 6, 2400.00, 'CREDIT', DATE '2026-01-31', 'Monthly salary'),
    (5, 2, 5, 4,   96.40, 'DEBIT',  DATE '2026-01-20', 'Electricity bill'),
    -- Ada — outside the window on both boundaries
    (6, 1, 3, 2,   51.10, 'DEBIT',  DATE '2026-02-01', 'February groceries'),
    (7, 1, 1, 5,    3.95, 'DEBIT',  DATE '2025-12-31', 'New Year coffee'),
    -- Ben — must never appear in Ada's results
    (8, 3, 3, 2,  140.00, 'DEBIT',  DATE '2026-01-15', 'Weekly groceries'),
    (9, 3, 2, 3,    5.50, 'DEBIT',  DATE '2026-01-16', 'Metro fare');

INSERT INTO spending_insights (id, user_id, period_start, period_end, summary) VALUES
    (1, 1, DATE '2026-01-01', DATE '2026-02-01', 'Groceries and utilities account for most January spending.');

-- Explicit keys do not advance an identity sequence; realign them so the
-- application can insert without colliding with the seed.
SELECT setval(pg_get_serial_sequence('users',             'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('accounts',          'id'), (SELECT max(id) FROM accounts));
SELECT setval(pg_get_serial_sequence('merchants',         'id'), (SELECT max(id) FROM merchants));
SELECT setval(pg_get_serial_sequence('categories',        'id'), (SELECT max(id) FROM categories));
SELECT setval(pg_get_serial_sequence('transactions',      'id'), (SELECT max(id) FROM transactions));
SELECT setval(pg_get_serial_sequence('spending_insights', 'id'), (SELECT max(id) FROM spending_insights));

COMMIT;
