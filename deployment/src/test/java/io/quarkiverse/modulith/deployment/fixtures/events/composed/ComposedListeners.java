package io.quarkiverse.modulith.deployment.fixtures.events.composed;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ComposedListeners {

    public static final List<String> HITS = new CopyOnWriteArrayList<>();

    @SyncListener
    void direct(Shipped event) {
        HITS.add("direct:" + event.id() + ":" + Thread.currentThread().getName());
    }

    @AuditListener
    void nested(Shipped event) {
        HITS.add("nested:" + event.id() + ":" + Thread.currentThread().getName());
    }

    @DoubleListener
    void twice(Shipped event) {
        HITS.add("twice:" + event.id() + ":" + Thread.currentThread().getName());
    }
}
