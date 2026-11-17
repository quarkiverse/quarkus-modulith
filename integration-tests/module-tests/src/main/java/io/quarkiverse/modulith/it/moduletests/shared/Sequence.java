package io.quarkiverse.modulith.it.moduletests.shared;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Sequence {

    private final AtomicInteger next = new AtomicInteger();

    public int next() {
        return next.incrementAndGet();
    }
}
