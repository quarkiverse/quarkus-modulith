package io.quarkiverse.modulith.core.fixtures.kinds.k;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.core.fixtures.kinds.other.Other;

public class Fired {

    final Event<KEvent> events;

    Other other;

    @Inject
    Fired(Event<KEvent> events, Event<String> strings) {
        this.events = events;
    }

    void take(KEvent[] events, int count, String name) {
    }

    @Inject
    void setOther(Other other) {
        this.other = other;
    }
}
