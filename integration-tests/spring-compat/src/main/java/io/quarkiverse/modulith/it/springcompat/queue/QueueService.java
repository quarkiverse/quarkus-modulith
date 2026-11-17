package io.quarkiverse.modulith.it.springcompat.queue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the queue module.
 */
@ApplicationScoped
public class QueueService {

    @Inject
    Clock clock;

    public String name() {
        return "queue";
    }
}
