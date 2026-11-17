package io.quarkiverse.modulith.flyway.runtime;

import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;

/**
 * The migration files of another resource provider that lie below one of the given class path folders. Built on the
 * provider quarkus-flyway configures from the files it collected at build time, so it works in native executables.
 */
public final class LocationResourceProvider implements ResourceProvider {

    private final ResourceProvider resources;
    private final List<String> folders;

    /**
     * @param resources the provider of every migration file
     * @param folders the folders to keep, each ending with {@code /}
     */
    public LocationResourceProvider(ResourceProvider resources, List<String> folders) {
        this.resources = resources;
        this.folders = List.copyOf(folders);
    }

    @Override
    public LoadableResource getResource(String name) {
        return resources.getResource(name);
    }

    @Override
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
        return resources.getResources(prefix, suffixes).stream()
                .filter(resource -> folders.stream().anyMatch(resource.getAbsolutePath()::startsWith))
                .toList();
    }

    /**
     * @return whether at least one file lies below the folders, Java migration class files included, as Spring
     *         Modulith's {@code HasMigrationFiles} checks
     */
    public boolean hasFiles() {
        return !getResources("", new String[] { "" }).isEmpty();
    }
}
