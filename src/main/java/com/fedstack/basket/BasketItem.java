package com.fedstack.basket;

import java.math.BigDecimal;

public class BasketItem {
    private final Product product;
    private final int quantity;

    public BasketItem(Product product) {
        this(product, 1);
    }

    public BasketItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Basket item product is required.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Basket item quantity must be at least 1.");
        }
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return Money.scale(product.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
    }

    @Override
    public String toString() {
        return quantity + " x " + product.getName() + " = $" + getLineTotal();
    }
}
