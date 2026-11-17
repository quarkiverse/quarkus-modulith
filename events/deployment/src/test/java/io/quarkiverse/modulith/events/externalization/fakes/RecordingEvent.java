package io.quarkiverse.modulith.events.externalization.fakes;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

public class RecordingEvent<T> implements Event<T> {

    private final List<T> fired = new CopyOnWriteArrayList<>();
    private final RuntimeException failure;

    public RecordingEvent(RuntimeException failure) {
        this.failure = failure;
    }

    public List<T> fired() {
        return List.copyOf(fired);
    }

    @Override
    public void fire(T event) {
        fired.add(event);
        if (Objects.nonNull(failure)) {
            throw failure;
        }
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }
}
