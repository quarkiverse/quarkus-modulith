package io.quarkiverse.modulith.flyway.runtime;

import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;

public final class FixedResourceProvider implements ResourceProvider {

    private final List<LoadableResource> resources;

    public FixedResourceProvider(List<LoadableResource> resources) {
        this.resources = List.copyOf(resources);
    }

    @Override
    public LoadableResource getResource(String name) {
        return resources.stream().filter(resource -> resource.getAbsolutePath().equals(name)).findFirst().orElseThrow();
    }

    @Override
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
        return resources;
    }
}
