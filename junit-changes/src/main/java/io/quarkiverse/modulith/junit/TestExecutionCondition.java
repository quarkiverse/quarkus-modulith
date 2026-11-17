package io.quarkiverse.modulith.junit;

import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;

/**
 * Decides whether to run a test class based on a {@link ConditionContext}, independent of JUnit's extension context.
 * A test runs if a class path resource or build file changed, if no class changed (unless configured otherwise), if the
 * test class itself changed, if it resides in a root module, in no module, in a changed module or in a module that
 * depends on a changed module directly or transitively.
 */
final class TestExecutionCondition {

    private static final System.Logger LOG = System.getLogger(TestExecutionCondition.class.getName());

    private final Function<String, ApplicationModules> modulesFactory;
    private final BasePackageFinder basePackageFinder;
    private final Map<String, ApplicationModules> modules = new ConcurrentHashMap<>();
    private final Map<String, RuntimeException> failures = new ConcurrentHashMap<>();
    private final Map<ApplicationModule, Set<String>> dependencies = new ConcurrentHashMap<>();

    /**
     * @param modulesFactory builds the application modules of a base package, {@link ApplicationModules#of(String)} in
     *        production
     * @param basePackageFinder finds the base package of the application a test class belongs to
     */
    TestExecutionCondition(Function<String, ApplicationModules> modulesFactory, BasePackageFinder basePackageFinder) {
        this.modulesFactory = modulesFactory;
        this.basePackageFinder = basePackageFinder;
    }

    ConditionEvaluationResult evaluate(ConditionContext context) {
        var changes = context.changes();
        if (changes.hasClasspathResourceChange()) {
            return enabled("Some classpath resource changed, running all tests.");
        }
        if (changes.hasBuildResourceChanges()) {
            return enabled("Build resource changed, running all tests.");
        }
        if (!changes.hasClassChanges()) {
            return changes.skipTestsOnNoChanges()
                    ? disabled("No source file changes detected, tests skipped due to configuration "
                            + "\"on-no-changes=skip-all\".")
                    : enabled("No source file changes detected, running full test suite due to default configuration.");
        }
        var changedClasses = changes.getChangedClasses();
        LOG.log(Level.TRACE, "Found following changed classes {0}", changedClasses);
        var testClass = context.testClass();
        if (changedClasses.contains(testClass.getName())) {
            return enabled("Change in test class detected, executing test.");
        }
        var basePackage = context.basePackage()
                .or(() -> basePackageFinder.find(testClass.getPackageName(), testClass.getClassLoader()));
        if (basePackage.isEmpty()) {
            return enabled("Unable to locate the application base package of %s. Annotate exactly one type with "
                    .formatted(testClass.getName())
                    + "@Modulith or set quarkus.modulith.base-package, executing test.");
        }
        var failure = failures.get(basePackage.get());
        if (Objects.nonNull(failure)) {
            return unavailable(basePackage.get(), failure);
        }
        ApplicationModules applicationModules;
        try {
            applicationModules = modules.computeIfAbsent(basePackage.get(), modulesFactory);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Unable to build the application module model of base package %s, executing "
                    .formatted(basePackage.get()) + "the tests of this application.", e);
            failures.put(basePackage.get(), e);
            return unavailable(basePackage.get(), e);
        }
        var packageName = testClass.getPackageName();
        return applicationModules.getModuleForPackage(packageName)
                .map(module -> evaluate(module, applicationModules, changedClasses))
                .orElseGet(() -> enabled("Test in package %s does not reside in any module!".formatted(packageName)));
    }

    private ConditionEvaluationResult evaluate(ApplicationModule module, ApplicationModules applicationModules,
            Set<String> changedClasses) {
        if (module.getBasePackage().equals(applicationModules.getBasePackage())) {
            return enabled("Always executing tests in root modules.");
        }
        var moduleDependencies = dependencies.computeIfAbsent(module,
                it -> allDependencies(it, applicationModules));
        for (var changedClass : changedClasses) {
            if (module.contains(changedClass)) {
                return enabled("Changes detected in module %s, executing test.".formatted(module.getName()));
            }
            var dependency = applicationModules.getModuleByType(changedClass)
                    .filter(it -> moduleDependencies.contains(it.getName()));
            if (dependency.isPresent()) {
                return enabled("Changes detected in dependent module %s, executing test."
                        .formatted(dependency.get().getName()));
            }
        }
        return disabled("Test residing in module %s not affected by changes!".formatted(module.getName()));
    }

    /**
     * @return the names of the modules the given module depends on, directly or transitively
     */
    static Set<String> allDependencies(ApplicationModule module, ApplicationModules applicationModules) {
        var result = new LinkedHashSet<String>();
        var pending = new ArrayDeque<>(module.getDependencies());
        while (!pending.isEmpty()) {
            var name = pending.poll();
            if (result.add(name)) {
                applicationModules.getModuleByName(name).ifPresent(it -> pending.addAll(it.getDependencies()));
            }
        }
        return result;
    }

    private static ConditionEvaluationResult unavailable(String basePackage, RuntimeException failure) {
        return enabled("Application module model of base package %s unavailable (%s), executing test."
                .formatted(basePackage, failure.getMessage()));
    }

    private static ConditionEvaluationResult enabled(String message) {
        LOG.log(Level.INFO, message);
        return ConditionEvaluationResult.enabled(message);
    }

    private static ConditionEvaluationResult disabled(String message) {
        LOG.log(Level.INFO, message);
        return ConditionEvaluationResult.disabled(message);
    }
}
