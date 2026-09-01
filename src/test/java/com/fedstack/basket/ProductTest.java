package com.fedstack.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void createsProductWithValidValues() {
        Product product = new Product(" mug-1 ", " Mug ", new BigDecimal("12.5"));

        assertEquals("mug-1", product.getId());
        assertEquals("Mug", product.getName());
        assertEquals(new BigDecimal("12.50"), product.getUnitPrice());
    }

    @Test
    void rejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> new Product(" ", "Mug", new BigDecimal("12.50")));
    }

    @Test
    void rejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> new Product("MUG-001", null, new BigDecimal("12.50")));
    }

    @Test
    void rejectsNullPrice() {
        assertThrows(IllegalArgumentException.class, () -> new Product("MUG-001", "Mug", null));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Product("MUG-001", "Mug", new BigDecimal("-0.01")));
    }
}
