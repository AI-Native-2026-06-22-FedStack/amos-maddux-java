# Shopping Basket Decisions

## What happens when the same product is added twice?

The basket stores each add operation as a separate basket item. This keeps the model simple and preserves exactly what the caller added.

## How did you keep callers from changing the basket's item list directly?

The basket owns a private mutable list and returns an unmodifiable copy from `getItems()`. A caller can inspect the returned list, but cannot add, remove, or replace basket items through it.

## Which relationship could have used inheritance, and why did you use composition instead?

A basket item could have been modeled as a kind of product with a quantity, but it is not really a product. A basket item HAS-A product, so composition keeps the product details separate from the shopper's chosen quantity.

## How can another discount type be added later?

Another discount can be added by creating a new class that implements `DiscountPolicy`. The basket will work with it through the same interface without changing basket code.
