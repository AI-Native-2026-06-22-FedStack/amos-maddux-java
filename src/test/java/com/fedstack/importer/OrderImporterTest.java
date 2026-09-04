package com.fedstack.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderImporterTest {
    private final OrderImporter importer = new OrderImporter();

    @Test
    void acceptsValidRowsAndReportsCorrectCount() {
        String input = String.join("\n",
                "O-100,Ada,25.00",
                "O-101,Ben,12.50");

        ImportResult result = importer.importOrders(new StringReader(input));

        assertEquals(2, result.getAcceptedCount());
        assertEquals("O-100", result.getOrders().get(0).getOrderId());
        assertEquals("Ada", result.getOrders().get(0).getCustomerName());
        assertEquals(new BigDecimal("25.00"), result.getOrders().get(0).getAmount());
        assertEquals("O-101", result.getOrders().get(1).getOrderId());
    }

    @Test
    void skipsBlankLines() {
        String input = "O-100,Ada,25.00\n\n\nO-101,Ben,12.50";

        ImportResult result = importer.importOrders(new StringReader(input));

        assertEquals(2, result.getAcceptedCount());
    }

    @Test
    void emptyInputProducesZeroAcceptedOrders() {
        ImportResult result = importer.importOrders(new StringReader(""));

        assertEquals(0, result.getAcceptedCount());
        assertTrue(result.getOrders().isEmpty());
    }

    @Test
    void malformedRowWithMissingFieldThrowsWithRowNumberAndNoFullRow() {
        String input = String.join("\n",
                "O-100,Ada,25.00",
                "O-101,Ben");

        MalformedOrderRowException ex = assertThrows(MalformedOrderRowException.class,
                () -> importer.importOrders(new StringReader(input)));

        assertEquals(2, ex.getRowNumber());
        assertTrue(ex.getMessage().contains("Row 2"));
        assertFalse(ex.getMessage().contains("O-101,Ben"));
    }

    @Test
    void blankCustomerNameIsRejected() {
        String input = "O-100, ,25.00";

        MalformedOrderRowException ex = assertThrows(MalformedOrderRowException.class,
                () -> importer.importOrders(new StringReader(input)));

        assertEquals(1, ex.getRowNumber());
    }

    @Test
    void negativeAmountIsRejected() {
        String input = "O-100,Ada,-5.00";

        MalformedOrderRowException ex = assertThrows(MalformedOrderRowException.class,
                () -> importer.importOrders(new StringReader(input)));

        assertEquals(1, ex.getRowNumber());
    }

    @Test
    void nonNumericAmountIsRejected() {
        String input = "O-100,Ada,not-a-number";

        MalformedOrderRowException ex = assertThrows(MalformedOrderRowException.class,
                () -> importer.importOrders(new StringReader(input)));

        assertEquals(1, ex.getRowNumber());
        assertFalse(ex.getMessage().contains("not-a-number"));
    }

    @Test
    void readFailureIsTranslatedAndPreservesCause() {
        IOException originalFailure = new IOException("disk unavailable");
        CloseTrackingReader failingReader =
                new CloseTrackingReader(new StringReader("O-100,Ada,25.00"), originalFailure);

        OrderSourceReadException ex = assertThrows(OrderSourceReadException.class,
                () -> importer.importOrders(failingReader));

        assertSame(originalFailure, ex.getCause());
        assertFalse(ex.getMessage().contains("O-100"));
    }

    @Test
    void sourceClosesAfterSuccessfulImport() {
        CloseTrackingReader reader = new CloseTrackingReader(new StringReader("O-100,Ada,25.00"));

        importer.importOrders(reader);

        assertTrue(reader.isClosed());
    }

    @Test
    void sourceClosesAfterMalformedRowFailure() {
        CloseTrackingReader reader = new CloseTrackingReader(new StringReader("O-100,Ada"));

        assertThrows(MalformedOrderRowException.class, () -> importer.importOrders(reader));

        assertTrue(reader.isClosed());
    }

    @Test
    void sourceClosesAfterReadFailure() {
        CloseTrackingReader reader =
                new CloseTrackingReader(new StringReader("O-100,Ada,25.00"), new IOException("boom"));

        assertThrows(OrderSourceReadException.class, () -> importer.importOrders(reader));

        assertTrue(reader.isClosed());
    }

    @Test
    void bothFailureTypesCanBeCaughtByTheSharedBaseType() {
        OrderImportException malformed = assertThrows(OrderImportException.class,
                () -> importer.importOrders(new StringReader("O-100,Ada")));
        assertInstanceOf(MalformedOrderRowException.class, malformed);

        OrderImportException readFailure = assertThrows(OrderImportException.class,
                () -> importer.importOrders(
                        new CloseTrackingReader(new StringReader("x"), new IOException("io error"))));
        assertInstanceOf(OrderSourceReadException.class, readFailure);
    }

    @Test
    void nullSourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> importer.importOrders(null));
    }
}
