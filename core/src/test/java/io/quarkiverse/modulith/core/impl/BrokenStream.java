package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.io.InputStream;

/**
 * A stream that fails on every read.
 */
class BrokenStream extends InputStream {

    @Override
    public int read() throws IOException {
        throw new IOException("broken");
    }
}
