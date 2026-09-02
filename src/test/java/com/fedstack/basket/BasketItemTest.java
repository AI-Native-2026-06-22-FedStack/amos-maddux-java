package com.fedstack.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BasketItemTest {
    @Test
    void defaultsQuantityToOne() {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));
        BasketItem item = new BasketItem(mug);

        assertEquals(1, item.getQuantity());
        assertEquals(new BigDecimal("12.50"), item.getLineTotal());
    }

    @Test
    void computesLineTotal() {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));
        BasketItem item = new BasketItem(mug, 2);

        assertEquals(new BigDecimal("25.00"), item.getLineTotal());
    }

    @Test
    void rejectsNullProduct() {
        assertThrows(IllegalArgumentException.class, () -> new BasketItem(null, 1));
    }

    @Test
    void rejectsQuantityBelowOne() {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));

        assertThrows(IllegalArgumentException.class, () -> new BasketItem(mug, 0));
    }
}
