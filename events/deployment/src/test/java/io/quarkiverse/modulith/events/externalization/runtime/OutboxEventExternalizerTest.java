package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.externalization.TestExternalizers;
import io.quarkiverse.modulith.events.externalization.fakes.DirectExecutor;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.IOThreadDetector;

class OutboxEventExternalizerTest {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    private final DirectExecutor executor = new DirectExecutor();
    private final RecordingDrain drain = new RecordingDrain();
    private final OutboxEventExternalizer outbox = new OutboxEventExternalizer();

    OutboxEventExternalizerTest() {
        outbox.configuration = TestExternalizers.STRINGS;
        outbox.publications = TestPublications.initialize(new ExternalizationPublications(), Optional.of(repository));
        outbox.transactions = new NoExternalizationTransactions();
        outbox.drain = drain;
        outbox.executor = executor;
    }

    @AfterEach
    void allowBlockingAgain() {
        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[0]);
    }

    @Test
    void recordsAndTriggersTheDrain() {
        // act
        outbox.onEvent("hello");

        // assert
        assertThat(repository.findIncompletePublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getListenerId())
                        .isEqualTo(OutboxEventExternalizer.LISTENER_ID));
        assertThat(drain.calls).containsExactly("trigger");
        assertThat(executor.executed()).isZero();
    }

    @Test
    void recordsOnAWorkerThreadWhenCalledOnAnIoThread() {
        // arrange
        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[] { () -> true });

        // act
        outbox.onEvent("hello");

        // assert
        assertThat(executor.executed()).isEqualTo(1);
        assertThat(repository.findIncompletePublications()).hasSize(1);
        assertThat(drain.calls).containsExactly("trigger");
    }

    @Test
    void ignoresEventsTheConfigurationDoesNotSelect() {
        // act
        outbox.onEvent(42);

        // assert
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(drain.calls).isEmpty();
    }

    @Test
    void resubmitsOnlyOutboxPublications() {
        // act
        outbox.resubmit(UUID.randomUUID(), "hello");

        // assert
        assertThat(drain.calls).containsExactly("trigger");
        assertThat(outbox.handles(OutboxEventExternalizer.LISTENER_ID)).isTrue();
        assertThat(outbox.handles(EventExternalizerModuleListener.LISTENER_ID)).isFalse();
    }
}
