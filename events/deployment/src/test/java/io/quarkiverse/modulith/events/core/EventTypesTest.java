package io.quarkiverse.modulith.events.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.tck.TckEvent;

class EventTypesTest {

    static final UUID PUBLICATION = new UUID(0, 42);

    private final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

    @AfterEach
    void restoreContextLoader() {
        Thread.currentThread().setContextClassLoader(contextLoader);
    }

    @Test
    void loadsStoredEventTypes() {
        // arrange
        String type = TckEvent.class.getName();

        // act
        Class<?> loaded = EventTypes.load(PUBLICATION, type);

        // assert
        assertThat(loaded).isEqualTo(TckEvent.class);
    }

    @Test
    void loadsThroughItsOwnClassLoaderOnThreadsWithoutContextLoader() {
        // arrange
        Thread.currentThread().setContextClassLoader(null);

        // act
        Class<?> loaded = EventTypes.load(PUBLICATION, TckEvent.class.getName());

        // assert
        assertThat(loaded).isEqualTo(TckEvent.class);
    }

    @Test
    void namesThePublicationWhenTheTypeIsGone() {
        // arrange
        String type = "com.acme.Removed";

        // act & assert
        assertThatThrownBy(() -> EventTypes.load(PUBLICATION, type)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read event publication " + PUBLICATION + " of type com.acme.Removed")
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void reportsAMissingTypeOnceAndFailsEveryRead() {
        // arrange
        String type = "com.acme.RemovedTwice";
        UUID other = new UUID(0, 43);

        // act
        Throwable first = catchThrowable(() -> EventTypes.load(PUBLICATION, type));
        Throwable second = catchThrowable(() -> EventTypes.load(other, type));

        // assert
        assertThat(first).hasMessage("Cannot read event publication " + PUBLICATION + " of type " + type);
        assertThat(second).hasMessage("Cannot read event publication " + other + " of type " + type);
        assertThat(EventTypes.REPORTED_MISSING).contains(type);
    }
}
