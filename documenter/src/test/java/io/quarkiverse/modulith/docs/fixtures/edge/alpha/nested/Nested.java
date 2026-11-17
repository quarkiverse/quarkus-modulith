package io.quarkiverse.modulith.docs.fixtures.edge.alpha.nested;

import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.docs.fixtures.edge.alpha.Alpha;

@Component
public class Nested {

    private final Alpha alpha;

    Nested(Alpha alpha) {
        this.alpha = alpha;
    }

    Alpha alpha() {
        return alpha;
    }
}
