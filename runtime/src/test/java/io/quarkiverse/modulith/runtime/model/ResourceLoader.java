package io.quarkiverse.modulith.runtime.model;

import java.io.InputStream;
import java.util.Map;

/**
 * Serves fixed resources, and none other.
 */
public class ResourceLoader extends ClassLoader {

    private final Map<String, InputStream> resources;

    public ResourceLoader(Map<String, InputStream> resources) {
        super(null);
        this.resources = resources;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return resources.get(name);
    }
}
