package io.quarkiverse.modulith.junit;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class loader that returns fixed resources for every name, or fails to list them, and loads no classes of its own.
 * It counts the resource lookups.
 */
class FixedResourcesClassLoader extends ClassLoader {

    private final List<URL> resources;
    private final Optional<IOException> failure;
    private final AtomicInteger lookups = new AtomicInteger();

    FixedResourcesClassLoader(List<URL> resources, Optional<IOException> failure) {
        super(ClassLoader.getPlatformClassLoader());
        this.resources = resources;
        this.failure = failure;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        lookups.incrementAndGet();
        if (failure.isPresent()) {
            throw failure.get();
        }
        return Collections.enumeration(resources);
    }

    int lookups() {
        return lookups.get();
    }
}
