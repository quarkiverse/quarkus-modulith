package io.quarkiverse.modulith.flyway.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.moduletest.ExcludedModulesBuildItem;
import io.quarkiverse.modulith.flyway.runtime.FlywayModules;
import io.quarkiverse.modulith.flyway.runtime.FlywayModulesCreator;
import io.quarkiverse.modulith.flyway.runtime.ModulithFlywayCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Build steps of {@code quarkus-modulith-flyway}: orders the modules by their dependencies, leaves out the modules a
 * module test does not start, and hooks the module migrations into the Flyway instance of the default datasource.
 */
class FlywayProcessor {

    static final String FEATURE = "modulith-flyway";

    static final String CLASSPATH_PREFIX = "classpath:";
    static final String FILESYSTEM_PREFIX = "filesystem:";

    static final Pattern MODULE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,39}");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = ModulithFlywayEnabled.class)
    void moduleMigrations(ModuleModelBuildItem model, ExcludedModulesBuildItem excluded, FlywayBuildTimeConfig flyway,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans, BuildProducer<AdditionalBeanBuildItem> beans) {
        List<String> modules = migrationOrder(Optional.ofNullable(model.getModel()), excluded);
        List<String> locations = flyway.datasources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).locations();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        checkIdentifiers(modules, module -> hasMigrations(locations, module,
                (folder, consumer) -> ClassPathUtils.consumeAsPaths(classLoader, folder, consumer)));
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(FlywayModules.class)
                .scope(Singleton.class)
                .creator(FlywayModulesCreator.class)
                .param(FlywayModulesCreator.MODULES, modules.toArray(String[]::new))
                .done());
        beans.produce(AdditionalBeanBuildItem.unremovableOf(ModulithFlywayCustomizer.class));
    }

    /**
     * Module identifiers become suffixes of history table names, so the identifiers of modules with migrations must be
     * plain identifiers that fit the table name limits of every database. Modules without migrations get no history
     * table, so their identifiers do not matter, such as the default identifiers of nested modules, which contain dots.
     *
     * @param hasMigrations tells whether a module has migration files
     */
    static void checkIdentifiers(List<String> modules, Predicate<String> hasMigrations) {
        List<String> invalid = modules.stream().filter(module -> !MODULE_IDENTIFIER.matcher(module).matches())
                .filter(hasMigrations).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalStateException("Modules " + invalid + " cannot get their own Flyway history table: a module "
                    + "identifier used with quarkus.modulith.flyway.enabled must start with a letter and have at most "
                    + "40 letters, digits or underscores. Change it with @ApplicationModule(id = ...).");
        }
    }

    /**
     * Looks for migration files the way quarkus-flyway collects them: in the folder of the module below each class path
     * location, SQL and Java migrations alike. File system locations are not migrated per module.
     *
     * @return whether the folder of the module holds a file below one of the locations
     */
    static boolean hasMigrations(List<String> locations, String module, ClassPath classPath) {
        return locations.stream().map(FlywayProcessor::classPathFolder).flatMap(Optional::stream)
                .anyMatch(folder -> hasFiles(folder + module, classPath));
    }

    /**
     * @return the class path folder of a location, without the {@code classpath:} prefix and slashes at the ends,
     *         empty for a file system location
     */
    static Optional<String> classPathFolder(String location) {
        if (location.startsWith(FILESYSTEM_PREFIX)) {
            return Optional.empty();
        }
        String folder = location.startsWith(CLASSPATH_PREFIX) ? location.substring(CLASSPATH_PREFIX.length())
                : location;
        folder = folder.replaceAll("^/+", "").replaceAll("/+$", "");
        return Optional.of(folder.isEmpty() ? "" : folder + "/");
    }

    static boolean hasFiles(String folder, ClassPath classPath) {
        AtomicBoolean found = new AtomicBoolean();
        try {
            classPath.consumeAsPaths(folder, path -> found.compareAndSet(false, containsFile(path)));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the Flyway migrations in " + folder, e);
        }
        return found.get();
    }

    static boolean containsFile(Path folder) {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the Flyway migrations in " + folder, e);
        }
    }

    /**
     * @return the names of the modules to migrate, every module after the modules it depends on, in model order
     *         otherwise; modules of a dependency cycle keep the model order
     */
    static List<String> migrationOrder(Optional<ModuleModel> model, ExcludedModulesBuildItem excluded) {
        List<ModuleDescriptor> pending = new ArrayList<>(model.map(ModuleModel::modules).orElse(List.of()));
        pending.removeIf(module -> excluded.getPackages().contains(module.basePackage()));
        Set<String> names = new LinkedHashSet<>();
        pending.forEach(module -> names.add(module.name()));
        Set<String> ordered = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            Optional<ModuleDescriptor> next = pending.stream()
                    .filter(module -> module.dependencies().stream()
                            .allMatch(dependency -> ordered.contains(dependency) || !names.contains(dependency)))
                    .findFirst();
            ModuleDescriptor module = next.orElse(pending.get(0));
            ordered.add(module.name());
            pending.remove(module);
        }
        return List.copyOf(ordered);
    }

    /**
     * Hands every class path directory or archive entry of a resource name to a consumer, like
     * {@link ClassPathUtils#consumeAsPaths(ClassLoader, String, Consumer)}.
     */
    @FunctionalInterface
    interface ClassPath {

        void consumeAsPaths(String resource, Consumer<Path> consumer) throws IOException;
    }
}
