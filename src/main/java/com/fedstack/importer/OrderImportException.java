package com.fedstack.importer;

/**
 * Base type for every failure raised while importing order rows.
 * Callers can catch this type to handle any import failure uniformly,
 * or catch a specific subtype to handle one failure kind alone.
 */
public abstract class OrderImportException extends RuntimeException {
    OrderImportException(String message) {
        super(message);
    }

    OrderImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
