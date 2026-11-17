package io.quarkiverse.modulith.runtime.events;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.EventMetadata;

record FakeEventContext(Object getEvent, Set<Annotation> qualifiers) implements EventContext<Object> {

    @Override
    public EventMetadata getMetadata() {
        return new FakeEventMetadata(qualifiers);
    }
}
