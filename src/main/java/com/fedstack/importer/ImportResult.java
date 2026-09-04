package com.fedstack.importer;

import java.util.List;

/**
 * Outcome of a successful import: the accepted orders and how many were accepted.
 */
public final class ImportResult {
    private final List<Order> orders;

    ImportResult(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public int getAcceptedCount() {
        return orders.size();
    }
}
