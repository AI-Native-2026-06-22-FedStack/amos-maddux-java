package com.fedstack.importer;

/**
 * Raised when the order source cannot be read at all (as opposed to a single
 * malformed row). Always wraps the original low-level failure as its cause.
 */
public final class OrderSourceReadException extends OrderImportException {
    OrderSourceReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
