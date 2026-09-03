package com.fedstack.importer;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable in-memory representation of one imported order row.
 */
public final class Order {
    private final String orderId;
    private final String customerName;
    private final BigDecimal amount;

    Order(String orderId, String customerName, BigDecimal amount) {
        if (isBlank(orderId)) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        if (isBlank(customerName)) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must not be negative.");
        }
        this.orderId = orderId.trim();
        this.customerName = customerName.trim();
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Order order)) {
            return false;
        }
        return orderId.equals(order.orderId)
                && customerName.equals(order.customerName)
                && amount.compareTo(order.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerName, amount);
    }

    @Override
    public String toString() {
        return "Order[" + orderId + "]";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
