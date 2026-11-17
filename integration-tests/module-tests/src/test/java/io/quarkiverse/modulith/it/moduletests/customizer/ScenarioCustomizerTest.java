package io.quarkiverse.modulith.it.moduletests.customizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;
import java.util.stream.Stream;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.test.EnableScenarios;
import io.quarkiverse.modulith.test.Scenario;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnableScenarios
@ExtendWith(ShortWait.class)
class ScenarioCustomizerTest {

    @Test
    void customizesTestMethods(Scenario scenario) {
        // act
        Throwable failure = catchThrowable(() -> scenario.stimulate(() -> {
        }).andWaitForStateChange(() -> null));

        // assert
        assertThat(failure).isInstanceOf(ConditionTimeoutException.class).hasMessageContaining("150 milliseconds");
        assertThat(ShortWait.CUSTOMIZED).contains("customizesTestMethods:true");
    }

    @ParameterizedTest
    @ValueSource(strings = { "first", "second" })
    void customizesTemplateMethods(String value, Scenario scenario) {
        // act
        Throwable failure = catchThrowable(() -> scenario.stimulate(() -> value)
                .andWaitForStateChange(() -> Stream.empty().toList()));

        // assert
        assertThat(failure).isInstanceOf(ConditionTimeoutException.class).hasMessageContaining("150 milliseconds");
        assertThat(ShortWait.CUSTOMIZED).contains("customizesTemplateMethods:true");
    }

    @TestFactory
    Stream<DynamicTest> customizesFactoryMethods(Scenario scenario) {
        // act
        Throwable failure = catchThrowable(() -> scenario.stimulate(() -> {
        }).andWaitForStateChange(Optional::empty));
        boolean customized = ShortWait.CUSTOMIZED.contains("customizesFactoryMethods:true");

        // assert
        return Stream.of(
                DynamicTest.dynamicTest("timeout", () -> assertThat(failure)
                        .isInstanceOf(ConditionTimeoutException.class).hasMessageContaining("150 milliseconds")),
                DynamicTest.dynamicTest("customized", () -> assertThat(customized).isTrue()));
    }
}
