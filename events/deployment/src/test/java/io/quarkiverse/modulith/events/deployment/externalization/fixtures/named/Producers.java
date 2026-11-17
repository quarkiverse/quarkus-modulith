package io.quarkiverse.modulith.events.deployment.externalization.fixtures.named;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

public class Producers {

    @Produces
    @Named("producedField")
    Thing field;

    @Produces
    @Named
    Thing defaultField;

    @Produces
    @Named("produced")
    Thing produce() {
        return null;
    }

    @Produces
    @Named
    Thing getProperty() {
        return null;
    }

    @Produces
    @Named
    Thing get() {
        return null;
    }

    @Named
    void notAProducer() {
    }
}
