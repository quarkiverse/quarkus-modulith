package io.quarkiverse.modulith.test;

import java.util.List;

class Aggregate {

    @DomainEvents
    String notACollection() {
        return "ignored";
    }

    @DomainEvents
    List<Object> withParameter(String ignored) {
        return List.of("ignored");
    }

    @Deprecated
    List<Object> otherAnnotation() {
        return List.of("ignored");
    }

    @DomainEvents
    private List<Object> events() {
        return List.of(new SampleCreated("c-1"), new SampleDeleted("d-1"));
    }
}
