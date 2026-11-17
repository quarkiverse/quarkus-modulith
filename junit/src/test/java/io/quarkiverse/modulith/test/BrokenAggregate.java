package io.quarkiverse.modulith.test;

import java.util.List;

class BrokenAggregate {

    @DomainEvents
    List<Object> events() {
        throw new IllegalArgumentException("broken");
    }
}
