package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A class loader whose resource lookup fails.
 */
class FailingClassLoader extends ClassLoader {

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new IOException("no resources");
    }
}
