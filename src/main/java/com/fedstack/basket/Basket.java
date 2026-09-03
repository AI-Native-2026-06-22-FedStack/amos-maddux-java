package com.fedstack.basket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Basket {
    private final List<BasketItem> items;

    public Basket() {
        this.items = new ArrayList<>();
    }

    public void addProduct(Product product) {
        items.add(new BasketItem(product));
    }

    public void addProduct(Product product, int quantity) {
        items.add(new BasketItem(product, quantity));
    }

    public List<BasketItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public BigDecimal getSubtotal() {
        BigDecimal subtotal = Money.ZERO;
        for (BasketItem item : items) {
            subtotal = subtotal.add(item.getLineTotal());
        }
        return Money.scale(subtotal);
    }

    public BigDecimal getTotal(DiscountPolicy discountPolicy) {
        if (discountPolicy == null) {
            throw new IllegalArgumentException("Discount policy is required.");
        }
        return Money.scale(discountPolicy.apply(getSubtotal()));
    }
}
