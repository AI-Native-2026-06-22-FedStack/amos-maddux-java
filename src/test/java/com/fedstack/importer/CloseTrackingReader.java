package com.fedstack.importer;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Test double that records whether {@code close()} was called and can be
 * configured to fail on read to simulate a lower-level read failure.
 */
final class CloseTrackingReader extends FilterReader {
    private final IOException failure;
    private boolean closed;

    CloseTrackingReader(Reader delegate) {
        this(delegate, null);
    }

    CloseTrackingReader(Reader delegate, IOException failure) {
        super(delegate);
        this.failure = failure;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (failure != null) {
            throw failure;
        }
        return super.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    boolean isClosed() {
        return closed;
    }
}
