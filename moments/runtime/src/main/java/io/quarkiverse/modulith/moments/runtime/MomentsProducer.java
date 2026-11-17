package io.quarkiverse.modulith.moments.runtime;

import java.time.Clock;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.moments.support.Moments;

/**
 * Produces the {@link Moments} bean (also injectable as {@code Now}). Registered by the deployment module when moments
 * are enabled and the time machine is not.
 */
@Singleton
public class MomentsProducer {

    @Produces
    @Singleton
    Moments moments(Instance<Clock> clocks, Event<Object> events, MomentsConfig config) {
        return new Moments(MomentsBeans.clock(clocks), events, MomentsBeans.properties(config, false));
    }
}
