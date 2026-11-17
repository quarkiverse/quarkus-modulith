package io.quarkiverse.modulith.events.externalization.impl;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRedelivery;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the {@link EventRedelivery} of applications without module listeners.
 */
public class ExternalizationRedeliveryCreator implements BeanCreator<EventRedelivery> {

    @Override
    public EventRedelivery create(SyntheticCreationalContext<EventRedelivery> context) {
        return new ExternalizationRedelivery(context.getInjectedReference(ExternalizationStrategy.class));
    }
}
