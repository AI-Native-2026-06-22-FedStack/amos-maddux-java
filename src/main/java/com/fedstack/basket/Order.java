package com.fedstack.basket;

import java.math.BigDecimal;

public final class Order {
    private final String id;
    private final Customer customer;
    private final OrderState state;
    private final BigDecimal amount;

    public Order(String id, Customer customer, OrderState state, BigDecimal amount) {
        if (isMissing(id)) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Order customer is required.");
        }
        if (state == null) {
            throw new IllegalArgumentException("Order state is required.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Order amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order amount cannot be negative.");
        }

        this.id = id.trim();
        this.customer = customer;
        this.state = state;
        this.amount = Money.scale(amount);
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderState getState() {
        return state;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return state == OrderState.PAID;
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
