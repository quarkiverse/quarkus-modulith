package io.quarkiverse.modulith.docs.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A class loader that cannot list resources.
 */
final class FailingClassLoader extends ClassLoader {

    FailingClassLoader() {
        super(null);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new IOException("broken");
    }
}
