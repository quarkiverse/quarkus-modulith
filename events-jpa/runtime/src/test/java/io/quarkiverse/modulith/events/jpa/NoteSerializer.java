package io.quarkiverse.modulith.events.jpa;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Stores a {@link Note} as its text and counts reads.
 */
final class NoteSerializer implements EventSerializer {

    final AtomicInteger reads = new AtomicInteger();

    @Override
    public Object serialize(Object event) {
        return ((Note) event).text();
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        reads.incrementAndGet();
        return type.cast(new Note(serialized.toString()));
    }
}
