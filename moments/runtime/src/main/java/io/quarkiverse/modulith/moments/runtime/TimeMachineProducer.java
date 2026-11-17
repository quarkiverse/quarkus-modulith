package io.quarkiverse.modulith.moments.runtime;

import java.time.Clock;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.moments.support.TimeMachine;

/**
 * Produces the {@link TimeMachine} bean (also injectable as {@code Moments} and {@code Now}). Registered by the
 * deployment module instead of {@link MomentsProducer} when {@code quarkus.modulith.moments.enable-time-machine=true}.
 */
@Singleton
public class TimeMachineProducer {

    @Produces
    @Singleton
    TimeMachine timeMachine(Instance<Clock> clocks, Event<Object> events, MomentsConfig config) {
        return new TimeMachine(MomentsBeans.clock(clocks), events, MomentsBeans.properties(config, true));
    }
}
