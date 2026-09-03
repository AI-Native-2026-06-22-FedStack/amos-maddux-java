package com.fedstack.basket;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
