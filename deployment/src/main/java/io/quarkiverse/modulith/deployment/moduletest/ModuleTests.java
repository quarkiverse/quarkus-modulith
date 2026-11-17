package io.quarkiverse.modulith.deployment.moduletest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.core.Violations;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.model.ModuleModelBuilder;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig.BootstrapMode;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;

/**
 * The build-time logic of module tests, used by {@code ModuleTestProcessor}.
 */
public final class ModuleTests {

    /**
     * The test-mode recorder of {@code quarkus-modulith-junit}. Referenced by name, the deployment module does not
     * depend on the test framework.
     */
    public static final String RECORDER = "io.quarkiverse.modulith.test.PublishedEventsRecorder";

    /**
     * {@code @ApplicationModuleTest} of {@code quarkus-modulith-junit}.
     */
    public static final String TEST_ANNOTATION = "io.quarkiverse.modulith.test.ApplicationModuleTest";

    /**
     * Read by {@code ModuleTestExecution} of {@code quarkus-modulith-junit}.
     */
    public static final String SETUP_RESOURCE = "META-INF/quarkus-modulith/module-test.properties";

    private static final Logger LOG = Logger.getLogger(ModuleTests.class);

    private ModuleTests() {
    }

    /**
     * @return the modules of the current module test, empty outside module tests
     * @throws IllegalStateException if the configuration names unknown modules
     * @throws Violations if {@code verify-automatically} is on and the application breaks a module rule
     */
    public static Optional<ModuleTestSetup> setup(Optional<ModuleModel> found, ModuleTestConfig config) {
        if (found.isEmpty() || config.module().isEmpty()) {
            return Optional.empty();
        }
        ModuleModel model = found.get();
        String requested = config.module().get();
        ModuleDescriptor tested = model.resolve(requested).orElseThrow(() -> new IllegalStateException(
                "quarkus.modulith.test.module=" + requested + " does not match any module of " + model.modules()));
        List<String> extra = config.extraModules().orElse(List.of());
        for (String name : extra) {
            if (model.module(name).isEmpty()) {
                throw new IllegalStateException("quarkus.modulith.test.extra-modules names " + name
                        + ", which is not a module of " + model.modules());
            }
        }
        BootstrapMode mode = config.bootstrapMode();
        Set<String> dependencies = dependencies(model, tested, mode);
        dependencies.remove(tested.name());
        Set<String> included = new LinkedHashSet<>(dependencies);
        included.add(tested.name());
        included.addAll(extra);
        included.addAll(model.sharedModules());
        Set<String> excluded = packages(model, it -> !included.contains(it.name()));
        Set<String> kept = packages(model, it -> included.contains(it.name()));
        kept.removeIf(it -> excluded.stream().noneMatch(outer -> it.startsWith(outer + ".")));
        ExcludedModulesBuildItem exclusion = new ExcludedModulesBuildItem(excluded, kept);
        ModuleTestSetup setup = new ModuleTestSetup(tested.name(), mode, List.copyOf(dependencies), extra,
                model.sharedModules(), excluded, kept, excludedTypes(model, exclusion));
        log(tested, setup);
        if (config.verifyAutomatically()) {
            new Violations(model.violations()).throwIfPresent();
        }
        return Optional.of(setup);
    }

    /**
     * @return the bean of the event recorder, if {@code quarkus-modulith-junit} is present. It needs no unremovable
     *         flag, ArC keeps beans that declare observers
     */
    public static Optional<AdditionalBeanBuildItem> recorderBean(Predicate<String> present) {
        if (!present.test(RECORDER)) {
            return Optional.empty();
        }
        return Optional.of(AdditionalBeanBuildItem.builder().addBeanClass(RECORDER).build());
    }

    /**
     * @return {@code @ApplicationModuleTest} as a test annotation, if {@code quarkus-modulith-junit} is present
     */
    public static Optional<TestAnnotationBuildItem> testAnnotation(Predicate<String> present) {
        return present.test(TEST_ANNOTATION) ? Optional.of(new TestAnnotationBuildItem(TEST_ANNOTATION))
                : Optional.empty();
    }

    private static Set<String> packages(ModuleModel model, Predicate<ModuleDescriptor> filter) {
        return model.modules().stream().filter(filter).map(ModuleDescriptor::basePackage)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @return {@code package.**} for an excluded module without started nested module, otherwise one
     *         {@code package.*} per package of its own types, so that the nested module keeps its beans
     */
    static Set<String> excludedTypes(ModuleModel model, ExcludedModulesBuildItem exclusion) {
        Set<String> patterns = new LinkedHashSet<>();
        for (ModuleDescriptor module : model.modules()) {
            String base = module.basePackage();
            if (!exclusion.getPackages().contains(base)) {
                continue;
            }
            if (exclusion.getKeptPackages().stream().noneMatch(it -> it.startsWith(base + "."))) {
                patterns.add(base + ".**");
                continue;
            }
            patterns.add(base + ".*");
            module.classes().stream().filter(exclusion::excludes).map(ModuleModelBuilder::packageOf)
                    .forEach(it -> patterns.add(it + ".*"));
        }
        return patterns;
    }

    private static Set<String> dependencies(ModuleModel model, ModuleDescriptor tested, BootstrapMode mode) {
        Set<String> result = new LinkedHashSet<>();
        if (mode == BootstrapMode.STANDALONE) {
            return result;
        }
        result.addAll(tested.dependencies());
        if (mode == BootstrapMode.ALL_DEPENDENCIES) {
            List<String> queue = new ArrayList<>(tested.dependencies());
            while (!queue.isEmpty()) {
                model.module(queue.remove(0)).map(ModuleDescriptor::dependencies)
                        .ifPresent(next -> next.stream().filter(result::add).forEach(queue::add));
            }
        }
        return result;
    }

    private static void log(ModuleDescriptor tested, ModuleTestSetup setup) {
        LOG.infof("Bootstrapping module test for '%s' in mode %s, starting modules %s", setup.module(), setup.mode(),
                setup.includedModules());
        LOG.debugf("Module under test: %s", tested);
        LOG.debugf("Dependencies: %s", setup.dependencies());
        LOG.debugf("Extra modules: %s", setup.extraModules());
        LOG.debugf("Shared modules: %s", setup.sharedModules());
        LOG.debugf("Excluded module packages: %s", setup.excludedPackages());
        LOG.debugf("Kept nested module packages: %s", setup.keptPackages());
    }
}
