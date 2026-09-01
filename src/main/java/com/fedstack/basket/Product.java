package com.fedstack.basket;

import java.math.BigDecimal;

public class Product {
    private final String id;
    private final String name;
    private final BigDecimal unitPrice;

    public Product(String id, String name, BigDecimal unitPrice) {
        if (isMissing(id)) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        if (isMissing(name)) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Product unit price is required.");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product unit price cannot be negative.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.unitPrice = Money.scale(unitPrice);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Override
    public String toString() {
        return name + " (" + id + ") - $" + unitPrice;
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
