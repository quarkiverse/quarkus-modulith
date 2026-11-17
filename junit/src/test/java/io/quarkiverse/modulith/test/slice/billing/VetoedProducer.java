package io.quarkiverse.modulith.test.slice.billing;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class VetoedProducer {

    @Produces
    Long number() {
        return 1L;
    }
}
