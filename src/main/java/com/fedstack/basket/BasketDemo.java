package com.fedstack.basket;

import java.math.BigDecimal;

public class BasketDemo {
    public static void main(String[] args) {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));
        Product notebook = new Product("NOTE-001", "Notebook", new BigDecimal("5.00"));

        Basket basket = new Basket();
        basket.addProduct(mug, 2);
        basket.addProduct(notebook);

        DiscountPolicy noDiscount = new NoDiscountPolicy();
        DiscountPolicy tenPercentOff = new PercentageDiscountPolicy(new BigDecimal("10"));

        System.out.println("Subtotal: $" + basket.getSubtotal());
        System.out.println("No discount total: $" + basket.getTotal(noDiscount));
        System.out.println("10% off total: $" + basket.getTotal(tenPercentOff));
    }
}
