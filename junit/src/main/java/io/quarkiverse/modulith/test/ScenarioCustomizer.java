package io.quarkiverse.modulith.test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

/**
 * Customizes the Awaitility condition of every {@link Scenario} a test method receives, for example to wait longer by
 * default. Register an implementation with {@code @ExtendWith} on the test class:
 *
 * <pre>
 * class WaitLonger implements ScenarioCustomizer {
 *
 *     &#64;Override
 *     public Function&lt;ConditionFactory, ConditionFactory&gt; getDefaultCustomizer(Method method, ArcContainer container) {
 *         return it -&gt; it.atMost(Duration.ofMinutes(1));
 *     }
 * }
 * </pre>
 *
 * Constraints, because {@code @QuarkusTest} invokes test methods itself and skips later invocation interceptors, so
 * the customizers are applied when the {@link Scenario} parameter is resolved:
 * <ul>
 * <li>Customizers are found only through {@code @ExtendWith} on the test class, its superclasses, the classes that
 * enclose a {@code @Nested} test class, the test method, and their meta-annotations. Customizers registered with
 * {@code @RegisterExtension} are ignored.</li>
 * <li>They are instantiated again for every scenario, separately from the instance JUnit registers, so they need a
 * no-argument constructor and must not keep instance state.</li>
 * <li>The last customizer found wins; customizers are not combined.</li>
 * </ul>
 */
public interface ScenarioCustomizer extends InvocationInterceptor {

    /**
     * @param method the test method
     * @param container the running application's container, {@code null} if none runs
     * @return the customizer the scenarios of the test method start from, not {@code null}
     */
    Function<ConditionFactory, ConditionFactory> getDefaultCustomizer(Method method, ArcContainer container);

    /**
     * @return a customizer that polls on the application's {@link ExecutorService} bean, or no customization when the
     *         application has none
     */
    static Function<ConditionFactory, ConditionFactory> forwardExecutorService(ArcContainer container) {
        InstanceHandle<ExecutorService> executor = container.instance(ExecutorService.class);
        if (!executor.isAvailable()) {
            return Function.identity();
        }
        PollingExecutor service = new PollingExecutor(executor.get());
        return it -> it.pollExecutorService(service);
    }

    @Override
    default void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        prepareScenarios(invocationContext);
        invocation.proceed();
    }

    @Override
    default <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        prepareScenarios(invocationContext);
        return invocation.proceed();
    }

    @Override
    default void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        prepareScenarios(invocationContext);
        invocation.proceed();
    }

    private void prepareScenarios(ReflectiveInvocationContext<Method> invocationContext) {
        invocationContext.getArguments().stream()
                .filter(Scenario.class::isInstance)
                .map(Scenario.class::cast)
                .forEach(it -> it.setDefaultCustomizer(
                        getDefaultCustomizer(invocationContext.getExecutable(), Arc.container())));
    }
}
