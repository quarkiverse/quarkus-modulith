package io.quarkiverse.modulith.moments.support;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Consumer;

import jakarta.enterprise.event.Event;

/**
 * A {@link Moments} whose time can be shifted. The extension registers it in place of {@link Moments} when
 * {@code quarkus.modulith.moments.enable-time-machine=true}, so injecting {@link Moments}, {@link Now} or
 * {@link TimeMachine} returns the same instance. Intended for tests and local development.
 *
 * @see #shiftBy(Duration)
 * @see #reset()
 */
public class TimeMachine extends Moments {

    /**
     * Creates a new {@link TimeMachine} for the given {@link Clock}, CDI {@link Event} and {@link MomentsProperties}.
     *
     * @param clock must not be {@literal null}
     * @param events must not be {@literal null}
     * @param properties must not be {@literal null}
     */
    public TimeMachine(Clock clock, Event<Object> events, MomentsProperties properties) {
        super(clock, events, properties);
    }

    TimeMachine(Clock clock, Consumer<Object> events, MomentsProperties properties) {
        super(clock, events, properties);
    }

    /**
     * Moves the time forward and publishes the events of every boundary of the configured granularity on the way, in
     * chronological order. A negative duration moves the time backward and publishes nothing.
     *
     * @param duration must not be {@literal null}
     * @return this instance
     */
    @Override
    public TimeMachine shiftBy(Duration duration) {
        super.shiftBy(duration);
        return this;
    }

    /**
     * Drops every shift, so that the time is the one of the {@link Clock} again. Publishes nothing.
     *
     * @return this instance
     */
    @Override
    public TimeMachine reset() {
        super.reset();
        return this;
    }
}
