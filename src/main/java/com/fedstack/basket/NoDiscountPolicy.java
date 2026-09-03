package com.fedstack.basket;

import java.math.BigDecimal;

public class NoDiscountPolicy implements DiscountPolicy {
    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        if (subtotal == null) {
            throw new IllegalArgumentException("Subtotal is required.");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative.");
        }
        return Money.scale(subtotal);
    }
}
