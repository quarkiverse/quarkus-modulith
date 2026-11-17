package io.quarkiverse.modulith.flyway.runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.flyway.MigrationFilter;
import io.quarkus.arc.All;
import io.quarkus.flyway.FlywayConfigurationCustomizer;

/**
 * Turns the Flyway instance of the default datasource into the root migration: its locations are narrowed to the
 * {@code __root} folders, it gets a baseline at version 0, and a callback runs the module migrations after it. The
 * migration therefore follows quarkus-flyway's own triggers ({@code migrate-at-start}, {@code Flyway#migrate()}) and
 * runs before Hibernate ORM starts.
 * <p>
 * The startup fails for configurations the module migrations cannot honor: wildcard locations, which would show the
 * root migration the migrations of every module, and history table names longer than {@value #MAX_TABLE_NAME}
 * characters, the PostgreSQL limit.
 */
@Singleton
public class ModulithFlywayCustomizer implements FlywayConfigurationCustomizer {

    static final int MAX_TABLE_NAME = 63;
    static final String BASELINE_ON_MIGRATE = "quarkus.flyway.baseline-on-migrate";
    static final String BASELINE_VERSION = "quarkus.flyway.baseline-version";
    private static final String BASELINE = "0";
    private static final Logger LOG = Logger.getLogger(ModulithFlywayCustomizer.class);

    private final FlywayModules modules;
    private final List<MigrationFilter> filters;
    private final Config config;

    @Inject
    public ModulithFlywayCustomizer(FlywayModules modules, @All List<MigrationFilter> filters, Config config) {
        this.modules = modules;
        this.filters = filters;
        this.config = config;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
        Location[] locations = configuration.getLocations();
        checkLocations(locations);
        checkHistoryTables(configuration.getTable());
        warnAboutTheBaseline();
        // FlywayCreator of quarkus-flyway sets both providers before it calls the customizers, which is not a
        // contract. Tested with Quarkus 3.40 and 4.0.0.CR1.
        ResourceProvider resources = Objects.requireNonNull(configuration.getResourceProvider(),
                "quarkus-flyway called the customizer before setting the resource provider of the migrations");
        ClassProvider<JavaMigration> classes = Objects.requireNonNull(configuration.getJavaMigrationClassProvider(),
                "quarkus-flyway called the customizer before setting the class provider of the Java migrations");
        configuration.locations(MigrationLocations.in(locations, MigrationLocations.ROOT))
                .baselineVersion(BASELINE)
                .baselineOnMigrate(true)
                .callbacks(Stream.concat(Arrays.stream(configuration.getCallbacks()),
                        Stream.of(new ModuleMigrations(modules.identifiers(), locations, resources, classes, filters)))
                        .toArray(Callback[]::new));
        MigrationLocations.narrow(configuration, resources, classes);
    }

    private static void checkLocations(Location[] locations) {
        List<String> wildcards = wildcards(locations);
        if (!wildcards.isEmpty()) {
            throw new IllegalStateException("The Flyway locations " + wildcards + " contain wildcards, which would"
                    + " show the root migration the migrations of every module: list the locations without wildcards"
                    + " in quarkus.flyway.locations, or set quarkus.modulith.flyway.enabled=false");
        }
    }

    private void checkHistoryTables(String table) {
        List<String> tooLong = modules.identifiers().stream().map(module -> table + "_" + module)
                .filter(name -> name.length() > MAX_TABLE_NAME).toList();
        if (!tooLong.isEmpty()) {
            throw new IllegalStateException("The Flyway history tables " + tooLong + " have more than "
                    + MAX_TABLE_NAME + " characters: shorten quarkus.flyway.table or the module identifiers");
        }
    }

    private void warnAboutTheBaseline() {
        if (overridesTheBaseline(config.getOptionalValue(BASELINE_ON_MIGRATE, Boolean.class),
                config.getOptionalValue(BASELINE_VERSION, String.class))) {
            LOG.warnf("quarkus.modulith.flyway.enabled migrates the root and every module with a baseline at version"
                    + " 0, so %s and %s are ignored: migrations above version 0 run on an existing schema", BASELINE_ON_MIGRATE,
                    BASELINE_VERSION);
        }
    }

    /**
     * @return the locations with a Flyway wildcard ({@code *}, {@code **} or {@code ?})
     */
    static List<String> wildcards(Location[] locations) {
        return Arrays.stream(locations).map(Location::toString)
                .filter(location -> location.contains("*") || location.contains("?")).toList();
    }

    /**
     * @return whether the configured baseline settings differ from the baseline the module migrations use
     */
    static boolean overridesTheBaseline(Optional<Boolean> baselineOnMigrate, Optional<String> baselineVersion) {
        return !baselineOnMigrate.orElse(true) || baselineVersion.filter(version -> !BASELINE.equals(version)).isPresent();
    }
}
