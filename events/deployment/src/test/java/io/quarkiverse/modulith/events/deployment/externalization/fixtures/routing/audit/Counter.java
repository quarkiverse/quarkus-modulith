package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("counters::{event.value}")
public class Counter {

    public int value;

    public Counter(int value) {
        this.value = value;
    }
}
