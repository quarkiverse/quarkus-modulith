package io.quarkiverse.modulith.test.slice.billing;

import jakarta.enterprise.inject.Produces;

public class ProducerOnly {

    @Produces
    String name() {
        return "billing";
    }
}
