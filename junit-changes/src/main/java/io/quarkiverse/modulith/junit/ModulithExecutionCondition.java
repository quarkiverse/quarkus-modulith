package io.quarkiverse.modulith.junit;

import java.lang.StackWalker.StackFrame;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkiverse.modulith.core.ApplicationModules;

/**
 * JUnit Jupiter extension that skips the test classes of application modules that neither changed nor depend on a
 * changed module, like Spring Modulith's {@code ModulithExecutionCondition}. It works on the module model of
 * {@link ApplicationModules#of(String)} and the Git working tree, without starting Quarkus.
 * <p>
 * The extension is registered through the {@link java.util.ServiceLoader} file of this artifact and active as soon as
 * JUnit's extension auto detection is enabled ({@code junit.jupiter.extensions.autodetection.enabled=true} in the
 * project's {@code junit-platform.properties}). This artifact does not enable it itself, so that it never changes the
 * test runs of modules that merely have it on the class path. Alternatively register it with
 * {@code @ExtendWith(ModulithExecutionCondition.class)}.
 * <p>
 * The optimization backs off and runs the test when the tests are started from an IDE, when a class path resource or a
 * build file changed, when nothing changed, when the application base package or the module model is unavailable,
 * and for every single test method (the decision is made per test class).
 */
public class ModulithExecutionCondition implements ExecutionCondition {

    private static final List<String> IDE_PACKAGES = List.of("org.eclipse.jdt", "com.intellij");

    private static final boolean IDE_EXECUTION = StackWalker.getInstance()
            .walk(ModulithExecutionCondition::hasIdeRoot);

    private final boolean ideExecution;
    private final Supplier<Config> config;
    private final Path workingDirectory;
    private final TestExecutionCondition condition;

    /**
     * Creates the condition for the current working directory, used by JUnit.
     */
    public ModulithExecutionCondition() {
        this(IDE_EXECUTION, ModulithTestConfig::load, Path.of("").toAbsolutePath(), ApplicationModules::of);
    }

    /**
     * @param ideExecution whether the tests were started from an IDE
     * @param config loads the configuration
     * @param workingDirectory the directory of the project under test
     * @param modulesFactory builds the application modules of a base package
     */
    ModulithExecutionCondition(boolean ideExecution, Supplier<Config> config, Path workingDirectory,
            Function<String, ApplicationModules> modulesFactory) {
        this.ideExecution = ideExecution;
        this.config = config;
        this.workingDirectory = workingDirectory;
        this.condition = new TestExecutionCondition(modulesFactory, new BasePackageFinder());
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return evaluate(context.getTestMethod().isPresent(), context.getTestClass(),
                () -> new StateStore(context, config, workingDirectory).getSettings());
    }

    /**
     * @param methodContext whether JUnit evaluates a test method
     * @param testClass the test class, if any
     * @param settings the settings of the current test run, only obtained for a test class
     */
    ConditionEvaluationResult evaluate(boolean methodContext, Optional<Class<?>> testClass,
            Supplier<ExecutionSettings> settings) {
        if (ideExecution) {
            return ConditionEvaluationResult.enabled("Optimization disabled in IDE execution!");
        }
        if (methodContext) {
            return ConditionEvaluationResult.enabled("Not filtering on a per method basis!");
        }
        return testClass.map(type -> evaluate(type, settings.get()))
                .orElseGet(() -> ConditionEvaluationResult.enabled("Not a test class context!"));
    }

    private ConditionEvaluationResult evaluate(Class<?> testClass, ExecutionSettings settings) {
        var changes = settings.changes();
        return changes.isEmpty()
                ? ConditionEvaluationResult.enabled("No changes detected!")
                : condition.evaluate(new ConditionContext(testClass, changes, settings.basePackage()));
    }

    /**
     * @return whether the outermost frame of the stack belongs to an IDE test launcher
     */
    static boolean hasIdeRoot(Stream<StackFrame> frames) {
        return frames.reduce((first, second) -> second)
                .map(StackFrame::getClassName)
                .filter(ModulithExecutionCondition::isIdeClass)
                .isPresent();
    }

    /**
     * @param className a fully qualified class name
     * @return whether the class belongs to Eclipse's or IntelliJ IDEA's test launcher
     */
    static boolean isIdeClass(String className) {
        return IDE_PACKAGES.stream().anyMatch(className::startsWith);
    }
}
