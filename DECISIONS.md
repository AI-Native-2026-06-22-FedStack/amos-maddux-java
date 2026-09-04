# Shopping Basket Decisions

## What happens when the same product is added twice?

The basket stores each add operation as a separate basket item. This keeps the model simple and preserves exactly what the caller added.

## How did you keep callers from changing the basket's item list directly?

The basket owns a private mutable list and returns an unmodifiable copy from `getItems()`. A caller can inspect the returned list, but cannot add, remove, or replace basket items through it.

## Which relationship could have used inheritance, and why did you use composition instead?

A basket item could have been modeled as a kind of product with a quantity, but it is not really a product. A basket item HAS-A product, so composition keeps the product details separate from the shopper's chosen quantity.

## How can another discount type be added later?

Another discount can be added by creating a new class that implements `DiscountPolicy`. The basket will work with it through the same interface without changing basket code.

## Why is Customer a separate key type?

Order summaries need keyed revenue lookup by customer. `Customer` trims and validates the required name, then overrides `equals` and `hashCode` together so two customer instances with the same name collapse into one map entry.

## How are paid order results ordered?

The summary uses a chained comparator: paid orders are sorted by amount from highest to lowest, then by order ID from lowest to highest when amounts match. Customer totals are stored in a `LinkedHashMap` sorted by customer name so output is deterministic.

## How are summary results protected?

`OrderSummary` stores defensive unmodifiable copies of the paid details and customer revenue map. Customer lookups that may be absent return `Optional<BigDecimal>` instead of null.

# Safe Order Importer Decisions

## Why a separate package instead of extending the basket domain?

`com.fedstack.importer` is a standalone deliverable about exceptions, resource safety, and logging, not about shopping baskets. Keeping it in its own package avoids coupling two unrelated exercises and keeps each one independently readable and testable.

## Why does `importOrders` accept a `java.io.Reader`?

A `Reader` decouples the importer from any specific source (file, network, string) and makes read failures easy to simulate in tests. `CloseTrackingReader`, a test-only `FilterReader`, wraps a delegate to record whether `close()` ran and can be told to throw `IOException` on read, which is what lets tests assert both "the source closed" and "the original cause survived translation" without touching the filesystem.

## Why two exception subtypes under one base?

`MalformedOrderRowException` (bad row) and `OrderSourceReadException` (source unreadable) are different failure categories with different causes and different recovery stories, so they are distinct types. Both extend the abstract `OrderImportException`, so a caller that only cares "did the import fail" can catch the base type once, while a caller that wants to react differently to a bad row versus a dead source can catch the specific subtype.

## Where does exception translation happen, and how is the cause preserved?

`OrderImporter` is the only place low-level failures are translated. An `IOException` from `BufferedReader.readLine()` (or from `close()`) becomes an `OrderSourceReadException` constructed with the original exception as its cause, so `getCause()` still returns the original `IOException`. A malformed row's `IllegalArgumentException` (thrown by the `Order` constructor during validation) is translated the same way into a `MalformedOrderRowException`, but with the row number and a short reason instead of a wrapped low-level exception, since there is no lower-level failure to preserve there — the row itself is simply invalid.

## Why do exception messages and logs never include the full row?

The row can contain a customer name, which is customer data. Messages and logs report only what is useful for a caller or operator to act on (row number, expected vs. actual field count, "amount is not a valid number"), never the raw row text. `Order.toString()` follows the same rule and prints only the order ID, so an accidental `LOG.info("{}", order)` cannot leak customer data.

## How does try-with-resources guarantee the source always closes?

`OrderImporter.importOrders` wraps the given `Reader` in a `BufferedReader` inside a try-with-resources block. Every exit path — the normal return after all rows are read, a `MalformedOrderRowException` thrown while parsing a row, or an `OrderSourceReadException` thrown while reading — passes through the same block, so `close()` always runs before the exception propagates or the result returns.

## What logging levels were chosen, and why?

INFO marks expected lifecycle events an operator would want in normal operation: import started, import succeeded (with the accepted count). WARN marks a rejected row: unexpected, but the import can still finish handling the rest of the input in principle, and a single bad row is a data-quality issue, not a system fault. ERROR marks a read failure: the import cannot continue and the underlying cause is logged for diagnosis. All log calls use SLF4J parameter placeholders (`{}`) instead of string concatenation.
