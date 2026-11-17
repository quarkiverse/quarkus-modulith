package io.quarkiverse.modulith.flyway.runtime;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;

/**
 * Narrows Flyway locations to a sub folder and limits the migrations to those in the narrowed locations.
 */
public final class MigrationLocations {

    /**
     * The folder of migrations that belong to no module.
     */
    public static final String ROOT = "__root";

    private MigrationLocations() {
    }

    /**
     * @param folder a folder name, a module identifier or {@link #ROOT}; the customizer rejects wildcard locations
     * @return the locations with the folder appended
     */
    public static String[] in(Location[] locations, String folder) {
        return Arrays.stream(locations)
                .map(location -> location + "/" + folder)
                .toArray(String[]::new);
    }

    /**
     * Points the configuration's resource and Java migration providers at the migrations of its current locations,
     * taken from the providers of every migration.
     *
     * @return whether there is at least one file in the locations
     */
    public static boolean narrow(FluentConfiguration configuration, ResourceProvider resources,
            ClassProvider<JavaMigration> classes) {
        List<String> folders = Arrays.stream(configuration.getLocations())
                .map(location -> location.getRootPath() + "/")
                .toList();
        LocationResourceProvider narrowed = new LocationResourceProvider(resources, folders);
        configuration.resourceProvider(narrowed);
        configuration.javaMigrationClassProvider(new LocationClassProvider(classes, folders));
        return narrowed.hasFiles();
    }
}
