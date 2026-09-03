package com.fedstack.importer;

/**
 * Raised when a single order row cannot be parsed into valid order data.
 * The message reports the row number and a reason, never the row's full contents.
 */
public final class MalformedOrderRowException extends OrderImportException {
    private final int rowNumber;

    MalformedOrderRowException(int rowNumber, String reason) {
        super("Row " + rowNumber + " is malformed: " + reason);
        this.rowNumber = rowNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }
}
