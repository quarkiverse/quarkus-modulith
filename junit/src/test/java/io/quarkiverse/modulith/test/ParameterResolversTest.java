package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

class ParameterResolversTest {

    @ParameterizedTest
    @CsvSource({
            "io.quarkiverse.modulith.test.PublishedEvents, true",
            "io.quarkiverse.modulith.test.AssertablePublishedEvents, true",
            "io.quarkiverse.modulith.test.TypedPublishedEvents, false",
            "java.lang.String, false" })
    void publishedEventsResolverSupportsEventTypes(Class<?> type, boolean supported) {
        // arrange
        PublishedEventsParameterResolver resolver = new PublishedEventsParameterResolver(
                () -> new PublishedEventsRecorder("", false), true);

        // act
        boolean result = resolver.supportsParameter(parameter(type), mock(ExtensionContext.class));

        // assert
        assertThat(result).isEqualTo(supported);
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.PublishedEvents, true", "java.lang.String, false" })
    void publishedEventsResolverWithoutAssertJ(Class<?> type, boolean supported) {
        // arrange
        PublishedEventsParameterResolver resolver = new PublishedEventsParameterResolver(
                () -> new PublishedEventsRecorder("", false), false);

        // act
        boolean result = resolver.supportsParameter(parameter(type), mock(ExtensionContext.class));

        // assert
        assertThat(result).isEqualTo(supported);
    }

    @Test
    void assertableEventsNeedAssertJ() {
        // arrange
        PublishedEventsParameterResolver resolver = new PublishedEventsParameterResolver(
                () -> new PublishedEventsRecorder("", false), false);

        // act
        Throwable failure = catchThrowable(() -> resolver.supportsParameter(parameter(AssertablePublishedEvents.class),
                mock(ExtensionContext.class)));

        // assert
        assertThat(failure).isInstanceOf(ParameterResolutionException.class).hasMessage(
                "The test method declares an AssertablePublishedEvents parameter, but AssertJ is not on the class path");
    }

    @Test
    void registersPlainEventsBoundToTheThreadWithoutAssertJ() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", true);
        PublishedEventsParameterResolver resolver = new PublishedEventsParameterResolver(() -> recorder, false);
        EventsRegistration registration = resolver.register();

        // act
        recorder.onPublished("bound");

        // assert
        assertThat(registration.recorder()).isSameAs(recorder);
        assertThat(registration.events()).isNotInstanceOf(AssertablePublishedEvents.class);
        assertThat(registration.events().ofType(String.class)).containsExactly("bound");
    }

    @Test
    void closingTheRegistrationStopsRecording() throws Exception {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", true);
        EventsRegistration registration = new PublishedEventsParameterResolver(() -> recorder, false).register();
        AutoCloseable closedByJUnit = registration;
        recorder.onPublished("bound");

        // act
        closedByJUnit.close();
        recorder.onPublished("after");

        // assert
        assertThat(registration.events().ofType(String.class)).containsExactly("bound");
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.Scenario, true", "io.quarkiverse.modulith.test.PublishedEvents, false" })
    void scenarioResolverSupportsScenarios(Class<?> type, boolean supported) {
        // arrange
        ScenarioParameterResolver resolver = new ScenarioParameterResolver();

        // act
        boolean result = resolver.supportsParameter(parameter(type), mock(ExtensionContext.class));

        // assert
        assertThat(result).isEqualTo(supported);
    }

    @Test
    void scenarioNeedsAssertJ() {
        // arrange
        ScenarioParameterResolver resolver = new ScenarioParameterResolver(new FixedEventsResolver(), () -> null,
                false);

        // act
        Throwable failure = catchThrowable(
                () -> resolver.resolveParameter(parameter(Scenario.class), mock(ExtensionContext.class)));

        // assert
        assertThat(failure).isInstanceOf(ParameterResolutionException.class)
                .hasMessage("A Scenario needs AssertJ on the test class path");
    }

    @Test
    void scenarioOfAConstructorKeepsTheDefaultCustomizer() {
        // arrange
        FixedEventsResolver events = new FixedEventsResolver();
        ScenarioParameterResolver resolver = new ScenarioParameterResolver(events, ParameterResolversTest::container,
                true);
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestMethod()).thenReturn(Optional.empty());

        // act
        Scenario scenario = resolver.resolveParameter(parameter(Scenario.class), context);

        // assert
        assertThat(scenario.events()).isSameAs(events.events);
        assertThat(scenario.defaultCustomizer()).isSameAs(Function.identity());
        assertThat(scenario.transactions()).isInstanceOf(QuarkusTransactionOperations.class);
    }

    @Test
    void scenarioOfATestMethodTakesTheRegisteredCustomizers(TestInfo test) {
        // arrange
        ScenarioParameterResolver resolver = new ScenarioParameterResolver(new FixedEventsResolver(),
                ParameterResolversTest::container, true);
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestMethod()).thenReturn(test.getTestMethod());
        when(context.getRequiredTestClass()).thenAnswer(invocation -> CustomizedFixture.class);

        // act
        Scenario scenario = resolver.resolveParameter(parameter(Scenario.class), context);

        // assert
        assertThat(scenario.defaultCustomizer()).isSameAs(FixedWait.CUSTOMIZER);
    }

    @Test
    @ExtendWith(MethodWait.class)
    void customizersOfTheTestMethodWin(TestInfo test) {
        // arrange
        ScenarioParameterResolver resolver = new ScenarioParameterResolver(new FixedEventsResolver(),
                ParameterResolversTest::container, true);
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestMethod()).thenReturn(test.getTestMethod());
        when(context.getRequiredTestClass()).thenAnswer(invocation -> CustomizedFixture.class);

        // act
        Scenario scenario = resolver.resolveParameter(parameter(Scenario.class), context);

        // assert
        assertThat(scenario.defaultCustomizer()).isSameAs(MethodWait.CUSTOMIZER);
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.CustomizedFixture, 1",
            "io.quarkiverse.modulith.test.CustomizedFixture$Inner, 1",
            "io.quarkiverse.modulith.test.CustomizedFixture$Detached, 0",
            "io.quarkiverse.modulith.test.PlainProfile, 0" })
    void findsTheCustomizersOfTheTestClassAndTheClassesEnclosingIt(Class<?> testClass, int count, TestInfo test) {
        // act
        List<ScenarioCustomizer> customizers = ScenarioParameterResolver.customizers(testClass,
                test.getTestMethod().orElseThrow());

        // assert
        assertThat(customizers).hasSize(count).allMatch(FixedWait.class::isInstance);
    }

    @Test
    void moduleTestExtensionNeedsAModuleTestApplication() {
        // arrange
        ApplicationModuleTestExtension extension = new ApplicationModuleTestExtension(Optional::empty);
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getRequiredTestClass()).thenAnswer(invocation -> CustomizedFixture.class);

        // act
        Throwable failure = catchThrowable(() -> extension.beforeAll(context));

        // assert
        assertThat(failure).isInstanceOf(ExtensionConfigurationException.class).hasMessage(
                "@ApplicationModuleTest io.quarkiverse.modulith.test.CustomizedFixture runs against the whole "
                        + "application. Add @TestProfile with a subclass of ApplicationModuleTestProfile, so that "
                        + "Quarkus builds the application for the module test.");
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.ModuleTestExecution, true",
            "io.quarkiverse.modulith.test.Scenario, false" })
    void moduleTestExtensionResolvesTheModuleTestExecution(Class<?> type, boolean supported) {
        // arrange
        ApplicationModuleTestExtension extension = new ApplicationModuleTestExtension(Optional::empty);

        // act
        boolean result = extension.supportsParameter(parameter(type), mock(ExtensionContext.class));

        // assert
        assertThat(result).isEqualTo(supported);
    }

    @SuppressWarnings("unchecked")
    private static ArcContainer container() {
        ArcContainer container = mock(ArcContainer.class);
        BeanManager beanManager = mock(BeanManager.class);
        Event<Object> event = mock(Event.class);
        InstanceHandle<ExecutorService> executor = mock(InstanceHandle.class);
        when(container.beanManager()).thenReturn(beanManager);
        when(beanManager.getEvent()).thenReturn(event);
        when(container.instance(ExecutorService.class)).thenReturn(executor);
        return container;
    }

    private static ParameterContext parameter(Class<?> type) {
        ParameterContext context = mock(ParameterContext.class);
        Parameter parameter = mock(Parameter.class);
        when(context.getParameter()).thenReturn(parameter);
        when(parameter.getType()).thenAnswer(invocation -> type);
        return context;
    }
}
