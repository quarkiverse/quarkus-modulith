package io.quarkiverse.modulith.test;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

/**
 * How the running module test application was set up: the module under test, the bootstrap mode, and which modules
 * were started. The Quarkus counterpart of Spring Modulith's {@code ModuleTestExecution}. Declare a parameter of this
 * type in a test method of an {@link ApplicationModuleTest}, or call {@link #current()}.
 * <p>
 * Iterating it gives the started modules: the module under test, its dependencies according to the bootstrap mode,
 * the extra modules and the shared modules.
 */
public final class ModuleTestExecution implements Iterable<ApplicationModule> {

    /**
     * The class path resource the build writes for a module test.
     */
    public static final String RESOURCE = "META-INF/quarkus-modulith/module-test.properties";

    private final ApplicationModules modules;
    private final ApplicationModule module;
    private final BootstrapMode bootstrapMode;
    private final List<ApplicationModule> dependencies;
    private final List<ApplicationModule> extraIncludes;
    private final List<ApplicationModule> sharedModules;

    private ModuleTestExecution(ApplicationModules modules, Properties properties) {
        this.modules = modules;
        this.module = module(required(properties, "module"));
        this.bootstrapMode = BootstrapMode.valueOf(required(properties, "bootstrap-mode"));
        this.dependencies = modules(properties.getProperty("dependencies", ""));
        this.extraIncludes = modules(properties.getProperty("extra-includes", ""));
        this.sharedModules = modules(properties.getProperty("shared-modules", ""));
    }

    /**
     * @return the setup of the running module test, empty if the running application is not a module test
     * @throws IllegalStateException if no Quarkus application with Quarkus Modulith runs
     */
    public static Optional<ModuleTestExecution> current() {
        return current(Arc.container());
    }

    static Optional<ModuleTestExecution> current(ArcContainer container) {
        if (isNull(container)) {
            throw new IllegalStateException("No Quarkus application is running");
        }
        InstanceHandle<ApplicationModules> handle = container.instance(ApplicationModules.class);
        if (!handle.isAvailable()) {
            throw new IllegalStateException("The running application has no ApplicationModules bean. Module tests "
                    + "need an application with the Quarkus Modulith extension.");
        }
        return of(Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE), handle.get());
    }

    /**
     * @param resource the resource the build wrote, {@code null} outside module tests
     */
    static Optional<ModuleTestExecution> of(InputStream resource, ApplicationModules modules) {
        if (isNull(resource)) {
            return Optional.empty();
        }
        try (InputStream in = resource) {
            Properties properties = new Properties();
            properties.load(in);
            return Optional.of(new ModuleTestExecution(modules, properties));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    /**
     * @return the module under test
     */
    public ApplicationModule getModule() {
        return module;
    }

    public BootstrapMode getBootstrapMode() {
        return bootstrapMode;
    }

    /**
     * @return the modules started because of the bootstrap mode and the extra modules, without the module under test
     *         and the shared modules
     */
    public List<ApplicationModule> getDependencies() {
        Set<ApplicationModule> result = new LinkedHashSet<>(dependencies);
        result.addAll(extraIncludes);
        return List.copyOf(result);
    }

    /**
     * @return the modules started in addition, as requested by the test
     */
    public List<ApplicationModule> getExtraIncludes() {
        return extraIncludes;
    }

    /**
     * @return the shared modules, which every module test starts
     */
    public List<ApplicationModule> getSharedModules() {
        return sharedModules;
    }

    /**
     * @return all modules of the application, also those that were not started
     */
    public ApplicationModules getModules() {
        return modules;
    }

    /**
     * @return the base packages of the started modules
     */
    public List<String> getBasePackages() {
        List<String> packages = new ArrayList<>();
        forEach(it -> packages.add(it.getBasePackage()));
        return packages;
    }

    /**
     * @return whether the module with the given identifier was started
     */
    public boolean isIncludedInExecution(String moduleName) {
        return includedModules().stream().anyMatch(it -> it.getName().equals(moduleName));
    }

    /**
     * @return whether the class was part of the test application: it belongs to a started module or to no module
     */
    public boolean includes(String className) {
        return modules.getModuleForType(className).map(it -> isIncludedInExecution(it.getName())).orElse(true);
    }

    /**
     * Throws {@link io.quarkiverse.modulith.core.Violations} if the module rules are broken anywhere in the
     * application.
     */
    public void verify() {
        modules.verify();
    }

    @Override
    public Iterator<ApplicationModule> iterator() {
        return includedModules().iterator();
    }

    @Override
    public String toString() {
        return "ModuleTestExecution[module=" + module.getName() + ", mode=" + bootstrapMode + ", started="
                + includedModules().stream().map(ApplicationModule::getName).toList() + "]";
    }

    private List<ApplicationModule> includedModules() {
        Set<ApplicationModule> result = new LinkedHashSet<>();
        result.add(module);
        result.addAll(getDependencies());
        result.addAll(sharedModules);
        return List.copyOf(result);
    }

    private static String required(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key)).filter(Predicate.not(String::isBlank))
                .orElseThrow(() -> new IllegalStateException("%s has no %s entry. Rebuild the module test with the "
                        .formatted(RESOURCE, key) + "same version of Quarkus Modulith as the test."));
    }

    private ApplicationModule module(String name) {
        return modules.getModuleByName(name).orElseThrow(
                () -> new IllegalStateException("The module test names an unknown module " + name));
    }

    private List<ApplicationModule> modules(String names) {
        return Arrays.stream(names.split(",")).filter(it -> !it.isEmpty()).map(this::module).toList();
    }
}
