package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class VerificationTest {

    private static final String FAILURE = "custom rule broken";
    private static final Violation VIOLATION = new Violation(Violation.Kind.ADDITIONAL_RULE, "", "", "", FAILURE);

    private static ApplicationModules modules() {
        return new ApplicationModules(new ModelData("", "", List.of(), List.of(), ""));
    }

    @Test
    void evaluatesNewOptionsAfterTheDefaultVerificationPassed() {
        // arrange
        ApplicationModules modules = modules();
        modules.verify();
        VerificationOptions failing = VerificationOptions.defaults().withoutAdditionalVerifications()
                .andRules(it -> new Violations(List.of(VIOLATION)));

        // act & assert
        assertThatThrownBy(() -> modules.verify(failing)).isInstanceOf(Violations.class).hasMessage("- " + FAILURE);
    }

    @Test
    void failsAgainOnEveryCallWithFailingOptions() {
        // arrange
        ApplicationModules modules = modules();
        VerificationOptions failing = VerificationOptions.defaults().withoutAdditionalVerifications()
                .andRules(it -> new Violations(List.of(VIOLATION)));
        catchThrowable(() -> modules.verify(failing));

        // act & assert
        assertThatThrownBy(() -> modules.verify(failing)).isInstanceOf(Violations.class).hasMessage("- " + FAILURE);
    }

    @Test
    void evaluatesTheSameOptionsOnlyUntilTheyPass() {
        // arrange
        ApplicationModules modules = modules();
        AtomicInteger evaluations = new AtomicInteger();
        VerificationOptions counting = VerificationOptions.defaults().withoutAdditionalVerifications()
                .andRules(it -> {
                    evaluations.incrementAndGet();
                    return Violations.NONE;
                });

        // act
        ApplicationModules first = modules.verify(counting);
        ApplicationModules second = modules.verify(counting);

        // assert
        assertThat(first).isSameAs(modules);
        assertThat(second).isSameAs(modules);
        assertThat(evaluations).hasValue(1);
    }

    @Test
    void evaluatesEqualButDistinctOptionsAgain() {
        // arrange
        ApplicationModules modules = modules();
        AtomicInteger evaluations = new AtomicInteger();
        ModulithRule counting = it -> {
            evaluations.incrementAndGet();
            return Violations.NONE;
        };
        modules.verify(VerificationOptions.defaults().withoutAdditionalVerifications().andRules(counting));

        // act
        modules.verify(VerificationOptions.defaults().withoutAdditionalVerifications().andRules(counting));

        // assert
        assertThat(evaluations).hasValue(2);
    }

    @Test
    void returnsRightAwayOnceTheDefaultVerificationPassed() {
        // arrange
        ApplicationModules modules = modules();
        modules.verify();

        // act
        ApplicationModules verified = modules.verify();

        // assert
        assertThat(verified).isSameAs(modules);
    }
}
