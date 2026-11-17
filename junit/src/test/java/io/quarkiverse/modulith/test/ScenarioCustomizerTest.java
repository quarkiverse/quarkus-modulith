package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import jakarta.enterprise.event.Event;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

class ScenarioCustomizerTest {

    private static final String APPLICATION_THREAD = "application-executor";

    private final FixedWait customizer = new FixedWait();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, APPLICATION_THREAD));

    @AfterEach
    void stopExecutor() {
        executor.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void interceptedTestMethodsGetTheCustomizer() throws Throwable {
        // arrange
        Scenario scenario = scenario();
        Invocation<Void> invocation = mock(Invocation.class);

        // act
        customizer.interceptTestMethod(invocation, context(scenario), mock(ExtensionContext.class));

        // assert
        assertThat(scenario.defaultCustomizer()).isSameAs(FixedWait.CUSTOMIZER);
        verify(invocation).proceed();
    }

    @Test
    @SuppressWarnings("unchecked")
    void interceptedTemplateMethodsGetTheCustomizer() throws Throwable {
        // arrange
        Scenario scenario = scenario();
        Invocation<Void> invocation = mock(Invocation.class);

        // act
        customizer.interceptTestTemplateMethod(invocation, context(scenario), mock(ExtensionContext.class));

        // assert
        assertThat(scenario.defaultCustomizer()).isSameAs(FixedWait.CUSTOMIZER);
        verify(invocation).proceed();
    }

    @Test
    @SuppressWarnings("unchecked")
    void interceptedFactoryMethodsGetTheCustomizer() throws Throwable {
        // arrange
        Scenario scenario = scenario();
        Invocation<String> invocation = mock(Invocation.class);
        when(invocation.proceed()).thenReturn("tests");

        // act
        String result = customizer.interceptTestFactoryMethod(invocation, context(scenario),
                mock(ExtensionContext.class));

        // assert
        assertThat(result).isEqualTo("tests");
        assertThat(scenario.defaultCustomizer()).isSameAs(FixedWait.CUSTOMIZER);
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardsNothingWithoutExecutor() {
        // arrange
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<ExecutorService> handle = mock(InstanceHandle.class);
        when(container.instance(ExecutorService.class)).thenReturn(handle);

        // act
        Function<ConditionFactory, ConditionFactory> result = ScenarioCustomizer.forwardExecutorService(container);

        // assert
        assertThat(result).isSameAs(Function.identity());
    }

    @Test
    @SuppressWarnings("unchecked")
    void pollsOnTheExecutorOfTheApplication() {
        // arrange
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<ExecutorService> handle = mock(InstanceHandle.class);
        when(container.instance(ExecutorService.class)).thenReturn(handle);
        when(handle.isAvailable()).thenReturn(true);
        when(handle.get()).thenReturn(executor);
        Function<ConditionFactory, ConditionFactory> customizer = ScenarioCustomizer.forwardExecutorService(container);
        AtomicReference<String> pollingThread = new AtomicReference<>();

        // act
        customizer.apply(Awaitility.await().atMost(Duration.ofSeconds(5)))
                .until(() -> pollingThread.compareAndSet(null, Thread.currentThread().getName()));

        // assert
        assertThat(pollingThread).hasValue(APPLICATION_THREAD);
    }

    @SuppressWarnings("unchecked")
    private static Scenario scenario() {
        return new Scenario(new DirectTransactionOperations(), mock(Event.class),
                (AssertablePublishedEvents) DefaultAssertablePublishedEvents.create());
    }

    @SuppressWarnings("unchecked")
    private static ReflectiveInvocationContext<Method> context(Scenario scenario) {
        ReflectiveInvocationContext<Method> context = mock(ReflectiveInvocationContext.class);
        when(context.getArguments()).thenReturn(List.of("other", scenario));
        when(context.getExecutable()).thenReturn(mock(Method.class));
        return context;
    }
}
