package com.fedstack.basket;

import java.math.BigDecimal;

public interface DiscountPolicy {
    BigDecimal apply(BigDecimal subtotal);
}
