package io.quarkiverse.modulith.test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Resolves {@link Scenario} test method parameters. The scenario shares the published events of the test with
 * {@link PublishedEvents} parameters.
 */
class ScenarioParameterResolver implements ParameterResolver, AfterEachCallback {

    private final PublishedEventsParameterResolver events;
    private final Supplier<ArcContainer> container;
    private final boolean assertJPresent;

    /**
     * Called by JUnit.
     */
    public ScenarioParameterResolver() {
        this(new PublishedEventsParameterResolver(), Arc::container, ClassPresence.isPresent(ClassPresence.ASSERTJ));
    }

    ScenarioParameterResolver(PublishedEventsParameterResolver events, Supplier<ArcContainer> container,
            boolean assertJPresent) {
        this.events = events;
        this.container = container;
        this.assertJPresent = assertJPresent;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == Scenario.class;
    }

    @Override
    public Scenario resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (!assertJPresent) {
            throw new ParameterResolutionException("A Scenario needs AssertJ on the test class path");
        }
        AssertablePublishedEvents published = (AssertablePublishedEvents) events.resolve(extensionContext);
        ArcContainer arc = container.get();
        Scenario scenario = new Scenario(TransactionOperations.create(), arc.beanManager().getEvent(), published)
                .setDefaultCustomizer(ScenarioCustomizer.forwardExecutorService(arc));
        extensionContext.getTestMethod().ifPresent(method -> customizers(extensionContext.getRequiredTestClass(), method)
                .forEach(it -> scenario.setDefaultCustomizer(it.getDefaultCustomizer(method, arc))));
        return scenario;
    }

    /**
     * Instantiates the {@link ScenarioCustomizer} extensions registered with {@code @ExtendWith} on the classes that
     * enclose a {@code @Nested} test class, outermost first, on the test class and on the test method, in the order
     * JUnit registers them. {@code @QuarkusTest} invokes test methods itself and does not run further invocation
     * interceptors, so the customizers are applied when the scenario is resolved.
     */
    static List<ScenarioCustomizer> customizers(Class<?> testClass, Method testMethod) {
        return Stream.concat(enclosingClasses(testClass), Stream.of(testMethod))
                .flatMap(ScenarioParameterResolver::extensions)
                .filter(ScenarioCustomizer.class::isAssignableFrom)
                .distinct()
                .map(it -> (ScenarioCustomizer) ReflectionSupport.newInstance(it))
                .toList();
    }

    /**
     * @return the test class and, for an inner class, the classes enclosing it, outermost first
     */
    private static Stream<Class<?>> enclosingClasses(Class<?> testClass) {
        Class<?> enclosing = testClass.getEnclosingClass();
        boolean inner = Objects.nonNull(enclosing) && !Modifier.isStatic(testClass.getModifiers());
        return inner ? Stream.concat(enclosingClasses(enclosing), Stream.of(testClass)) : Stream.of(testClass);
    }

    private static Stream<Class<? extends Extension>> extensions(AnnotatedElement element) {
        return AnnotationSupport.findRepeatableAnnotations(element, ExtendWith.class).stream()
                .flatMap(it -> Arrays.stream(it.value()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        events.afterEach(context);
    }
}
