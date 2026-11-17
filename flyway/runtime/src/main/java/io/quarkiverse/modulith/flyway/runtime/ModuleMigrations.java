package io.quarkiverse.modulith.flyway.runtime;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.core.ApplicationModuleIdentifier;
import io.quarkiverse.modulith.flyway.MigrationFilter;

/**
 * Runs the migrations of each module after the root migrations, like Spring Modulith's
 * {@code SpringModulithFlywayMigrationStrategy}: a Flyway instance per module with the configuration of the root
 * migration, the module's folder below each original location, a history table named after the root one plus an
 * underscore and the module name, and a baseline at version 0 for schemas that already hold other modules' tables.
 * <p>
 * Only {@code migrate} reaches the modules: {@code validate-at-start}, {@code repair-at-start} and
 * {@code Flyway#validate()}, {@code repair()} or {@code info()} cover the root history table only, so a changed module
 * migration shows on the next migration.
 */
public final class ModuleMigrations implements Callback {

    private static final Logger LOG = Logger.getLogger(ModuleMigrations.class);

    private final List<String> modules;
    private final Location[] locations;
    private final ResourceProvider resources;
    private final ClassProvider<JavaMigration> classes;
    private final List<MigrationFilter> filters;

    /**
     * @param modules the modules in dependency order
     * @param locations the configured locations, before they were narrowed to the root folder
     * @param resources the provider of every migration file of the configured locations
     * @param classes the provider of every Java migration of the configured locations
     * @param filters the filters every module must pass
     */
    public ModuleMigrations(List<String> modules, Location[] locations, ResourceProvider resources,
            ClassProvider<JavaMigration> classes, List<MigrationFilter> filters) {
        this.modules = List.copyOf(modules);
        this.locations = locations.clone();
        this.resources = resources;
        this.classes = classes;
        this.filters = List.copyOf(filters);
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        for (String module : modules) {
            FluentConfiguration configuration = Flyway.configure()
                    .configuration(context.getConfiguration())
                    .callbacks(Arrays.stream(context.getConfiguration().getCallbacks())
                            .filter(callback -> !(callback instanceof ModuleMigrations))
                            .toArray(Callback[]::new))
                    .locations(MigrationLocations.in(locations, module))
                    .table(context.getConfiguration().getTable() + "_" + module)
                    .baselineVersion("0")
                    .baselineOnMigrate(true);
            boolean hasMigrations = MigrationLocations.narrow(configuration, resources, classes);
            Flyway flyway = configuration.load();
            ApplicationModuleIdentifier identifier = ApplicationModuleIdentifier.of(module);
            if (hasMigrations && filters.stream().allMatch(filter -> filter.shouldMigrate(identifier, flyway))) {
                LOG.debugf("Running the Flyway migrations of application module %s", module);
                flyway.migrate();
            }
        }
    }

    @Override
    public String getCallbackName() {
        return "Quarkus Modulith module migrations";
    }
}
