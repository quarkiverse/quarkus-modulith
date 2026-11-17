package io.quarkiverse.modulith.flyway.runtime;

import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.migration.JavaMigration;

/**
 * The Java migrations of another class provider whose package lies below one of the given class path folders.
 */
public final class LocationClassProvider implements ClassProvider<JavaMigration> {

    private final ClassProvider<JavaMigration> classes;
    private final List<String> folders;

    /**
     * @param classes the provider of every Java migration
     * @param folders the folders to keep, each ending with {@code /}
     */
    public LocationClassProvider(ClassProvider<JavaMigration> classes, List<String> folders) {
        this.classes = classes;
        this.folders = List.copyOf(folders);
    }

    @Override
    public Collection<Class<? extends JavaMigration>> getClasses() {
        return classes.getClasses().stream()
                .filter(type -> folders.stream().anyMatch(type.getName().replace('.', '/')::startsWith))
                .toList();
    }
}
