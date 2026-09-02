package com.fedstack.basket;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OrderSummaryService {
    private static final Comparator<Order> PAID_ORDER_ORDERING = Comparator
            .comparing(Order::getAmount, Comparator.reverseOrder())
            .thenComparing(Order::getId);

    private OrderSummaryService() {
    }

    public static OrderSummary summarize(Collection<Order> orders) {
        validateOrders(orders);
        rejectDuplicateIds(orders);

        List<Order> paidOrders = orders.stream()
                .filter(Order::isPaid)
                .sorted(PAID_ORDER_ORDERING)
                .toList();

        List<OrderDetail> paidOrderDetails = paidOrders.stream()
                .map(order -> new OrderDetail(order.getId(), order.getCustomer(), order.getAmount()))
                .toList();

        BigDecimal totalPaidRevenue = paidOrders.stream()
                .map(Order::getAmount)
                .reduce(Money.ZERO, BigDecimal::add);

        Map<Customer, BigDecimal> paidRevenueByCustomer = paidOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getCustomer,
                        Collectors.reducing(Money.ZERO, Order::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Customer::getName)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Money.scale(entry.getValue()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        return new OrderSummary(paidOrderDetails, totalPaidRevenue, paidRevenueByCustomer);
    }

    private static void validateOrders(Collection<Order> orders) {
        if (orders == null) {
            throw new IllegalArgumentException("Orders collection is required.");
        }
        if (orders.stream().anyMatch(order -> order == null)) {
            throw new IllegalArgumentException("Orders collection cannot contain null entries.");
        }
    }

    private static void rejectDuplicateIds(Collection<Order> orders) {
        Map<String, Long> countsById = orders.stream()
                .map(Order::getId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        boolean hasDuplicateId = countsById.values().stream()
                .anyMatch(count -> count > 1);
        if (hasDuplicateId) {
            throw new IllegalArgumentException("Duplicate order IDs are not allowed.");
        }
    }
}
