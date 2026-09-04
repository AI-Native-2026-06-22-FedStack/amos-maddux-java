package com.fedstack.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads order rows of the form {@code orderId,customerName,amount} from a
 * character source and turns each valid row into an {@link Order}.
 */
public final class OrderImporter {
    private static final Logger LOG = LoggerFactory.getLogger(OrderImporter.class);
    private static final int EXPECTED_FIELD_COUNT = 3;

    public ImportResult importOrders(Reader source) {
        if (source == null) {
            throw new IllegalArgumentException("Source reader is required.");
        }

        LOG.info("Starting order import.");

        try (BufferedReader reader = new BufferedReader(source)) {
            List<Order> orders = new ArrayList<>();
            int rowNumber = 0;
            String line;

            while (true) {
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    LOG.error("Order import failed while reading the source.", e);
                    throw new OrderSourceReadException("Failed to read order source.", e);
                }

                if (line == null) {
                    break;
                }

                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                orders.add(parseRow(rowNumber, line));
            }

            LOG.info("Order import succeeded. accepted={}", orders.size());
            return new ImportResult(orders);
        } catch (IOException e) {
            LOG.error("Order import failed while closing the source.", e);
            throw new OrderSourceReadException("Failed to close order source.", e);
        }
    }

    private Order parseRow(int rowNumber, String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != EXPECTED_FIELD_COUNT) {
            LOG.warn("Order import rejected row {}: expected {} fields but found {}.",
                    rowNumber, EXPECTED_FIELD_COUNT, fields.length);
            throw new MalformedOrderRowException(rowNumber,
                    "expected " + EXPECTED_FIELD_COUNT + " fields but found " + fields.length);
        }

        String orderId = fields[0].trim();
        String customerName = fields[1].trim();
        String rawAmount = fields[2].trim();

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount);
        } catch (NumberFormatException e) {
            LOG.warn("Order import rejected row {}: amount is not a valid number.", rowNumber);
            throw new MalformedOrderRowException(rowNumber, "amount is not a valid number");
        }

        try {
            return new Order(orderId, customerName, amount);
        } catch (IllegalArgumentException e) {
            LOG.warn("Order import rejected row {}: {}", rowNumber, e.getMessage());
            throw new MalformedOrderRowException(rowNumber, e.getMessage());
        }
    }
}
