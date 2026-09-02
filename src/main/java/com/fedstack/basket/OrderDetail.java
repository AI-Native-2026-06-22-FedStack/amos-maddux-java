package com.fedstack.basket;

import java.math.BigDecimal;

public final class OrderDetail {
    private final String orderId;
    private final Customer customer;
    private final BigDecimal amount;

    public OrderDetail(String orderId, Customer customer, BigDecimal amount) {
        if (isMissing(orderId)) {
            throw new IllegalArgumentException("Order detail ID is required.");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Order detail customer is required.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Order detail amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order detail amount cannot be negative.");
        }

        this.orderId = orderId.trim();
        this.customer = customer;
        this.amount = Money.scale(amount);
    }

    public String getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return orderId + " - " + amount;
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
