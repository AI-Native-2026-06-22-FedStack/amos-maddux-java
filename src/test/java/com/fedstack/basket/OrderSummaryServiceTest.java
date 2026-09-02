package com.fedstack.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderSummaryServiceTest {
    private static final Customer ADA = new Customer("Ada");
    private static final Customer BEN = new Customer("Ben");

    @Test
    void includesOnlyPaidOrdersAndTotalsOnlyPaidRevenue() {
        OrderSummary summary = OrderSummaryService.summarize(sampleOrders());

        assertEquals(List.of("O-100", "O-103", "O-102"), orderIds(summary));
        assertEquals(new BigDecimal("60.00"), summary.getTotalPaidRevenue());
        assertEquals(new BigDecimal("35.00"), summary.getPaidRevenueFor(new Customer("Ada")).orElseThrow());
        assertEquals(new BigDecimal("25.00"), summary.getPaidRevenueFor(new Customer("Ben")).orElseThrow());
    }

    @Test
    void ordersPaidDetailsByAmountDescendingThenIdAscending() {
        OrderSummary summary = OrderSummaryService.summarize(List.of(
                new Order("O-300", ADA, OrderState.PAID, new BigDecimal("25.00")),
                new Order("O-200", BEN, OrderState.PAID, new BigDecimal("25.00")),
                new Order("O-100", ADA, OrderState.PAID, new BigDecimal("30.00"))));

        assertEquals(List.of("O-100", "O-200", "O-300"), orderIds(summary));
    }

    @Test
    void customerTotalsAreScaledAndOrderedByCustomerName() {
        OrderSummary summary = OrderSummaryService.summarize(List.of(
                new Order("O-100", BEN, OrderState.PAID, new BigDecimal("1.005")),
                new Order("O-101", ADA, OrderState.PAID, new BigDecimal("2.005")),
                new Order("O-102", ADA, OrderState.PAID, new BigDecimal("3.00"))));

        assertEquals(List.of(new Customer("Ada"), new Customer("Ben")), List.copyOf(summary.getPaidRevenueByCustomer().keySet()));
        assertEquals(new BigDecimal("5.01"), summary.getPaidRevenueFor(new Customer("Ada")).orElseThrow());
        assertEquals(new BigDecimal("1.01"), summary.getPaidRevenueFor(new Customer("Ben")).orElseThrow());
    }

    @Test
    void emptyInputReturnsEmptyResultsAndZeroRevenue() {
        OrderSummary summary = OrderSummaryService.summarize(List.of());

        assertTrue(summary.getPaidOrderDetails().isEmpty());
        assertTrue(summary.getPaidRevenueByCustomer().isEmpty());
        assertEquals(new BigDecimal("0.00"), summary.getTotalPaidRevenue());
    }

    @Test
    void rejectsDuplicateOrderIds() {
        List<Order> orders = List.of(
                new Order("O-100", ADA, OrderState.PAID, new BigDecimal("1.00")),
                new Order("O-100", BEN, OrderState.UNPAID, new BigDecimal("2.00")));

        assertThrows(IllegalArgumentException.class, () -> OrderSummaryService.summarize(orders));
    }

    @Test
    void rejectsNullInputCollectionAndNullEntries() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order("O-100", ADA, OrderState.PAID, new BigDecimal("1.00")));
        orders.add(null);

        assertThrows(IllegalArgumentException.class, () -> OrderSummaryService.summarize(null));
        assertThrows(IllegalArgumentException.class, () -> OrderSummaryService.summarize(orders));
    }

    @Test
    void rejectsInvalidOrderAndCustomerValues() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(" "));
        assertThrows(IllegalArgumentException.class, () -> new Order(" ", ADA, OrderState.PAID, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new Order("O-100", null, OrderState.PAID, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new Order("O-100", ADA, null, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new Order("O-100", ADA, OrderState.PAID, null));
        assertThrows(IllegalArgumentException.class, () -> new Order("O-100", ADA, OrderState.PAID, new BigDecimal("-0.01")));
    }

    @Test
    void leavesInputCollectionUnchanged() {
        List<Order> orders = new ArrayList<>(sampleOrders());
        List<Order> originalOrder = List.copyOf(orders);

        OrderSummaryService.summarize(orders);

        assertEquals(originalOrder, orders);
    }

    @Test
    void returnedCollectionsCannotBeMutated() {
        OrderSummary summary = OrderSummaryService.summarize(sampleOrders());

        assertThrows(UnsupportedOperationException.class, () -> summary.getPaidOrderDetails().clear());
        assertThrows(UnsupportedOperationException.class, () -> summary.getPaidRevenueByCustomer().put(new Customer("Cal"), new BigDecimal("1.00")));
        assertEquals(new BigDecimal("60.00"), summary.getTotalPaidRevenue());
        assertEquals(3, summary.getPaidOrderDetails().size());
        assertEquals(2, summary.getPaidRevenueByCustomer().size());
    }

    @Test
    void equalCustomerInstancesCollapseIntoOneKeyedResult() {
        OrderSummary summary = OrderSummaryService.summarize(List.of(
                new Order("O-100", new Customer(" Ada "), OrderState.PAID, new BigDecimal("10.00")),
                new Order("O-101", new Customer("Ada"), OrderState.PAID, new BigDecimal("15.00"))));

        assertEquals(1, summary.getPaidRevenueByCustomer().size());
        assertEquals(new BigDecimal("25.00"), summary.getPaidRevenueFor(new Customer("Ada")).orElseThrow());
    }

    @Test
    void absentCustomerLookupReturnsEmptyOptional() {
        OrderSummary summary = OrderSummaryService.summarize(sampleOrders());

        assertFalse(summary.getPaidRevenueFor(new Customer("Cal")).isPresent());
    }

    @Test
    void customerWithOnlyUnpaidOrdersIsAbsentFromPaidRevenue() {
        Customer cal = new Customer("Cal");
        OrderSummary summary = OrderSummaryService.summarize(List.of(
                new Order("O-100", ADA, OrderState.PAID, new BigDecimal("10.00")),
                new Order("O-101", cal, OrderState.UNPAID, new BigDecimal("20.00"))));

        assertFalse(summary.getPaidRevenueFor(cal).isPresent());
        assertFalse(summary.getPaidRevenueByCustomer().containsKey(cal));
    }

    @Test
    void rejectsNullCustomerLookup() {
        OrderSummary summary = OrderSummaryService.summarize(sampleOrders());

        assertThrows(IllegalArgumentException.class, () -> summary.getPaidRevenueFor(null));
    }

    private static List<Order> sampleOrders() {
        return List.of(
                new Order("O-100", ADA, OrderState.PAID, new BigDecimal("25.00")),
                new Order("O-101", BEN, OrderState.UNPAID, new BigDecimal("40.00")),
                new Order("O-102", ADA, OrderState.PAID, new BigDecimal("10.00")),
                new Order("O-103", BEN, OrderState.PAID, new BigDecimal("25.00")));
    }

    private static List<String> orderIds(OrderSummary summary) {
        return summary.getPaidOrderDetails()
                .stream()
                .map(OrderDetail::getOrderId)
                .toList();
    }
}
