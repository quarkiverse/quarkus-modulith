package io.quarkiverse.modulith.test;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Checks that an {@link ApplicationModuleTest} runs against a module test application, and resolves
 * {@link ModuleTestExecution} parameters.
 */
class ApplicationModuleTestExtension implements BeforeAllCallback, ParameterResolver {

    private final Supplier<Optional<ModuleTestExecution>> execution;

    /**
     * Called by JUnit.
     */
    public ApplicationModuleTestExtension() {
        this(ModuleTestExecution::current);
    }

    ApplicationModuleTestExtension(Supplier<Optional<ModuleTestExecution>> execution) {
        this.execution = execution;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        resolve(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == ModuleTestExecution.class;
    }

    @Override
    public ModuleTestExecution resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return resolve(extensionContext);
    }

    private ModuleTestExecution resolve(ExtensionContext context) {
        return execution.get().orElseThrow(() -> new ExtensionConfigurationException(
                "@ApplicationModuleTest " + context.getRequiredTestClass().getName()
                        + " runs against the whole application. Add @TestProfile with a subclass of "
                        + "ApplicationModuleTestProfile, so that Quarkus builds the application for the module test."));
    }
}
