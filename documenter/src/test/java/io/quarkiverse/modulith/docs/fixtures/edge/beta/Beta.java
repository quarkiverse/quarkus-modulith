package io.quarkiverse.modulith.docs.fixtures.edge.beta;

import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.docs.fixtures.edge.alpha.api.AlphaApi;
import io.quarkiverse.modulith.docs.fixtures.edge.core.Clock;

@Component
public class Beta {

    private final AlphaApi alpha;
    private final Clock clock;

    Beta(AlphaApi alpha, Clock clock) {
        this.alpha = alpha;
        this.clock = clock;
    }

    AlphaApi alpha() {
        return alpha;
    }

    Clock clock() {
        return clock;
    }
}
