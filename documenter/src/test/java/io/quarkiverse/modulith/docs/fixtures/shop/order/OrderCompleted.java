package io.quarkiverse.modulith.docs.fixtures.shop.order;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record OrderCompleted(String id) {
}
