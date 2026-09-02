package com.fedstack.basket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BasketTest {
    @Test
    void emptyBasketReportsZeroSubtotalAndTotal() {
        Basket basket = new Basket();

        assertEquals(new BigDecimal("0.00"), basket.getSubtotal());
        assertEquals(new BigDecimal("0.00"), basket.getTotal(new NoDiscountPolicy()));
    }

    @Test
    void calculatesSubtotalForMultipleProducts() {
        Basket basket = sampleBasket();

        assertEquals(new BigDecimal("30.00"), basket.getSubtotal());
    }

    @Test
    void addProductSupportsDefaultAndExplicitQuantities() {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));
        Basket basket = new Basket();

        basket.addProduct(mug);
        basket.addProduct(mug, 2);

        assertEquals(2, basket.getItems().size());
        assertEquals(new BigDecimal("37.50"), basket.getSubtotal());
    }

    @Test
    void rejectsNullDiscountPolicy() {
        Basket basket = new Basket();

        assertThrows(IllegalArgumentException.class, () -> basket.getTotal(null));
    }

    @Test
    void returnedItemsCannotChangeBasketContents() {
        Basket basket = sampleBasket();
        List<BasketItem> items = basket.getItems();

        Product pen = new Product("PEN-001", "Pen", new BigDecimal("2.00"));
        assertThrows(UnsupportedOperationException.class, () -> items.add(new BasketItem(pen, 1)));
        assertEquals(2, basket.getItems().size());
        assertEquals(new BigDecimal("30.00"), basket.getSubtotal());
    }

    private static Basket sampleBasket() {
        Product mug = new Product("MUG-001", "Mug", new BigDecimal("12.50"));
        Product notebook = new Product("NOTE-001", "Notebook", new BigDecimal("5.00"));

        Basket basket = new Basket();
        basket.addProduct(mug, 2);
        basket.addProduct(notebook);
        return basket;
    }
}
