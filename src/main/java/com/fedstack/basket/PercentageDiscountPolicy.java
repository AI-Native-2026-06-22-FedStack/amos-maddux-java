package com.fedstack.basket;

import java.math.BigDecimal;

public class PercentageDiscountPolicy implements DiscountPolicy {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BigDecimal percentage;

    public PercentageDiscountPolicy(BigDecimal percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Discount percentage is required.");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        this.percentage = percentage;
    }

    @Override
    public BigDecimal apply(BigDecimal subtotal) {
        if (subtotal == null) {
            throw new IllegalArgumentException("Subtotal is required.");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative.");
        }

        BigDecimal discountMultiplier = ONE_HUNDRED.subtract(percentage).divide(ONE_HUNDRED);
        return Money.scale(subtotal.multiply(discountMultiplier));
    }
}
