package com.fedstack.basket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OrderSummary {
    private final List<OrderDetail> paidOrderDetails;
    private final BigDecimal totalPaidRevenue;
    private final Map<Customer, BigDecimal> paidRevenueByCustomer;

    OrderSummary(
            List<OrderDetail> paidOrderDetails,
            BigDecimal totalPaidRevenue,
            Map<Customer, BigDecimal> paidRevenueByCustomer) {
        this.paidOrderDetails = Collections.unmodifiableList(new ArrayList<>(paidOrderDetails));
        this.totalPaidRevenue = Money.scale(totalPaidRevenue);
        this.paidRevenueByCustomer = Collections.unmodifiableMap(new LinkedHashMap<>(paidRevenueByCustomer));
    }

    public List<OrderDetail> getPaidOrderDetails() {
        return paidOrderDetails;
    }

    public BigDecimal getTotalPaidRevenue() {
        return totalPaidRevenue;
    }

    public Map<Customer, BigDecimal> getPaidRevenueByCustomer() {
        return paidRevenueByCustomer;
    }

    public Optional<BigDecimal> getPaidRevenueFor(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        return Optional.ofNullable(paidRevenueByCustomer.get(customer));
    }
}
