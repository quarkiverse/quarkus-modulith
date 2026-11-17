package io.quarkiverse.modulith.runtime.model;

import java.io.IOException;
import java.io.InputStream;

public class BrokenStream extends InputStream {

    @Override
    public int read() throws IOException {
        throw new IOException("broken");
    }
}
