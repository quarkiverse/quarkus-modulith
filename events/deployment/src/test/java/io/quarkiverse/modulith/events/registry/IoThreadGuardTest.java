package io.quarkiverse.modulith.events.registry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.ResubmissionOptions;

class IoThreadGuardTest {

    @ParameterizedTest
    @MethodSource("calls")
    void refusesToBlockAnIoThread(Consumer<DefaultEventPublicationRegistry> call) {
        // arrange
        DefaultEventPublicationRegistry registry = new IoThreadRegistry();

        // act & assert
        assertThatThrownBy(() -> call.accept(registry)).isInstanceOf(IllegalStateException.class)
                .hasMessage("The event publication registry blocks on its store, call it from a worker or virtual"
                        + " thread, not from an I/O thread");
    }

    static List<Consumer<DefaultEventPublicationRegistry>> calls() {
        return List.of(DefaultEventPublicationRegistry::findIncompletePublications,
                registry -> registry.resubmitIncompletePublications(p -> true),
                registry -> registry.resubmitIncompletePublicationsOlderThan(Duration.ZERO),
                registry -> registry.resubmit(ResubmissionOptions.defaults()),
                registry -> registry.resubmitIncompletePublications(ResubmissionOptions.defaults()),
                DefaultEventPublicationRegistry::findAll,
                registry -> registry.deletePublications(p -> true),
                registry -> registry.deletePublicationsOlderThan(Duration.ZERO),
                DefaultEventPublicationRegistry::findAbandoned,
                registry -> registry.deleteAbandoned(p -> true),
                DefaultEventPublicationRegistry::markStalePublicationsFailed);
    }
}
