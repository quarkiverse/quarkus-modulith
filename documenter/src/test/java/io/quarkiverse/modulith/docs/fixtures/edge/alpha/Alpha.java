package io.quarkiverse.modulith.docs.fixtures.edge.alpha;

import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.docs.fixtures.edge.core.Clock;

@Component
public class Alpha {

    private final Clock clock;

    Alpha(Clock clock) {
        this.clock = clock;
    }

    Clock clock() {
        return clock;
    }
}
