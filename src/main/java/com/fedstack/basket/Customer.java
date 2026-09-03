package com.fedstack.basket;

import java.util.Objects;

public final class Customer {
    private final String name;

    public Customer(String name) {
        if (isMissing(name)) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer customer)) {
            return false;
        }
        return name.equals(customer.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
