package io.quarkiverse.modulith.runtime.events;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;

record FakeEventMetadata(Set<Annotation> getQualifiers) implements EventMetadata {

    @Override
    public InjectionPoint getInjectionPoint() {
        return null;
    }

    @Override
    public Type getType() {
        return Object.class;
    }
}
