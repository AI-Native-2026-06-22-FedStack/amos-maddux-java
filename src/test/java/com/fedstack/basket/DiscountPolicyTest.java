package com.fedstack.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DiscountPolicyTest {
    @Test
    void noDiscountLeavesSubtotalUnchanged() {
        DiscountPolicy policy = new NoDiscountPolicy();

        assertEquals(new BigDecimal("30.00"), policy.apply(new BigDecimal("30.00")));
    }

    @Test
    void percentageDiscountAppliesThroughInterface() {
        DiscountPolicy policy = new PercentageDiscountPolicy(new BigDecimal("10"));

        assertEquals(new BigDecimal("27.00"), policy.apply(new BigDecimal("30.00")));
    }

    @Test
    void rejectsInvalidPercentageValues() {
        assertThrows(IllegalArgumentException.class, () -> new PercentageDiscountPolicy(null));
        assertThrows(IllegalArgumentException.class, () -> new PercentageDiscountPolicy(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new PercentageDiscountPolicy(new BigDecimal("101")));
    }

    @Test
    void rejectsInvalidSubtotalInputs() {
        DiscountPolicy noDiscount = new NoDiscountPolicy();
        DiscountPolicy percentageDiscount = new PercentageDiscountPolicy(new BigDecimal("10"));

        assertThrows(IllegalArgumentException.class, () -> noDiscount.apply(null));
        assertThrows(IllegalArgumentException.class, () -> noDiscount.apply(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> percentageDiscount.apply(null));
        assertThrows(IllegalArgumentException.class, () -> percentageDiscount.apply(new BigDecimal("-0.01")));
    }
}
