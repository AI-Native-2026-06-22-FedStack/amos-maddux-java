package com.fedstack.basket;

import java.math.BigDecimal;
import java.util.List;

public class BasketDemo {
    public static void main(String[] args) {
        Customer ada = new Customer("Ada");
        Customer ben = new Customer("Ben");
        Customer cal = new Customer("Cal");

        List<Order> orders = List.of(
                new Order("O-100", ada, OrderState.PAID, new BigDecimal("25.00")),
                new Order("O-101", ben, OrderState.UNPAID, new BigDecimal("40.00")),
                new Order("O-102", ada, OrderState.PAID, new BigDecimal("10.00")),
                new Order("O-103", ben, OrderState.PAID, new BigDecimal("25.00")));

        OrderSummary summary = OrderSummaryService.summarize(orders);

        System.out.println("Paid order details:");
        summary.getPaidOrderDetails()
                .forEach(detail -> System.out.println(detail.getOrderId() + " - " + detail.getAmount()));

        System.out.println();
        System.out.println("Total paid revenue: " + summary.getTotalPaidRevenue());

        System.out.println();
        System.out.println("Paid revenue by customer:");
        summary.getPaidRevenueByCustomer()
                .forEach((customer, total) -> System.out.println(customer.getName() + " - " + total));

        OrderSummary emptySummary = OrderSummaryService.summarize(List.of());
        System.out.println();
        System.out.println("Empty paid order count: " + emptySummary.getPaidOrderDetails().size());
        System.out.println("Empty total paid revenue: " + emptySummary.getTotalPaidRevenue());

        System.out.println();
        summary.getPaidRevenueFor(cal)
                .ifPresentOrElse(
                        total -> System.out.println("Cal paid revenue: " + total),
                        () -> System.out.println("Cal paid revenue: absent"));
    }
}
