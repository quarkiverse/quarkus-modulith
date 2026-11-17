package io.quarkiverse.modulith.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

/**
 * A class loader that returns fixed resources for every name, or fails to list them, and loads no classes of its own.
 */
class FixedResourcesClassLoader extends ClassLoader {

    private final List<URL> resources;
    private final Optional<IOException> failure;

    FixedResourcesClassLoader(URL... resources) {
        super(null);
        this.resources = List.of(resources);
        this.failure = Optional.empty();
    }

    FixedResourcesClassLoader(IOException failure) {
        super(null);
        this.resources = List.of();
        this.failure = Optional.of(failure);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (failure.isPresent()) {
            throw failure.get();
        }
        return Collections.enumeration(resources);
    }
}
