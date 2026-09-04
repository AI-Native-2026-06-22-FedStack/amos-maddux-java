package com.fedstack.importer;

import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable example. Run with:
 * {@code mvn exec:java -Dexec.mainClass=com.fedstack.importer.OrderImportDemo}
 */
public final class OrderImportDemo {
    private static final Logger LOG = LoggerFactory.getLogger(OrderImportDemo.class);

    public static void main(String[] args) {
        String validRows = String.join("\n",
                "O-100,Ada,25.00",
                "O-101,Ben,12.50",
                "O-102,Cal,7.25");

        ImportResult result = new OrderImporter().importOrders(new StringReader(validRows));
        LOG.info("Demo: accepted {} order(s) from the valid batch.", result.getAcceptedCount());

        String batchWithMalformedRow = String.join("\n",
                "O-200,Dee,50.00",
                "O-201,Ben");

        try {
            new OrderImporter().importOrders(new StringReader(batchWithMalformedRow));
        } catch (MalformedOrderRowException e) {
            LOG.info("Demo: import failed safely - {}", e.getMessage());
        }
    }
}
