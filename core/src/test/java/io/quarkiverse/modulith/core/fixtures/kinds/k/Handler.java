package io.quarkiverse.modulith.core.fixtures.kinds.k;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public class Handler {

    @org.jmolecules.event.annotation.DomainEventHandler
    void handle(KEvent event) {
        event.touch();
    }

    @org.jmolecules.event.annotation.DomainEventHandler
    void handle(io.quarkiverse.modulith.core.fixtures.kinds.other.Other event, int count) {
    }

    @Async
    @EventListener(classes = KEntity.class)
    void withoutParameters() {
    }

    @io.quarkus.scheduler.Scheduled
    void tick() {
    }
}
