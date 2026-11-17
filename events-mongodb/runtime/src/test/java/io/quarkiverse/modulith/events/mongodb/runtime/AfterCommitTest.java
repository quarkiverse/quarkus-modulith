package io.quarkiverse.modulith.events.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;

class AfterCommitTest {

    @Test
    void skipsTheDeliveriesWhenTheMongoCommitFails() {
        // arrange
        ClientSession session = mock(ClientSession.class);
        when(session.hasActiveTransaction()).thenReturn(true);
        doThrow(new MongoException("commit failed")).when(session).commitTransaction();
        MongoDbEventPublicationRepository repository = new MongoDbEventPublicationRepository(new MongoStore(
                mock(MongoDatabase.class), new CountingSerializer(), () -> Optional.of(session), CompletionMode.UPDATE,
                UuidRepresentation.STANDARD));
        AtomicBoolean delivered = new AtomicBoolean();
        List<Runnable> scheduled = new ArrayList<>();
        repository.afterCommit(() -> delivered.set(true), scheduled::add);

        // act
        scheduled.get(0).run();

        // assert
        verify(session).commitTransaction();
        assertThat(delivered).isFalse();
    }
}
