package io.quarkiverse.modulith.events;

import java.util.Objects;

/**
 * Fired as a CDI event after an event was handed to the broker and the broker acknowledged it, like Spring Modulith's
 * {@code EventExternalized}. Observe it as {@code EventExternalized<?, ?>} and check {@link #getEventType()}.
 *
 * @param <S> the type of the source event
 * @param <T> the type of the result the broker returned, for example Kafka's {@code RecordMetadata}
 */
public final class EventExternalized<S, T> {

    private final S event;
    private final Object mapped;
    private final RoutingTarget target;
    private final T brokerResult;

    public EventExternalized(S event, Object mapped, RoutingTarget target, T brokerResult) {
        this.event = Objects.requireNonNull(event, "event");
        this.mapped = Objects.requireNonNull(mapped, "mapped");
        this.target = Objects.requireNonNull(target, "target");
        this.brokerResult = brokerResult;
    }

    /**
     * @return the event as it was published in the application
     */
    public S getEvent() {
        return event;
    }

    /**
     * @return the payload actually sent, after mapping
     */
    public Object getMapped() {
        return mapped;
    }

    /**
     * @return where the payload was sent, with expressions resolved
     */
    public RoutingTarget getTarget() {
        return target;
    }

    /**
     * @return what the broker returned, may be {@code null}
     */
    public T getBrokerResult() {
        return brokerResult;
    }

    /**
     * @return the type of the source event
     */
    @SuppressWarnings("unchecked")
    public Class<S> getEventType() {
        return (Class<S>) event.getClass();
    }

    /**
     * @return the type of the source event, same as {@link #getEventType()}
     */
    public Class<S> getType() {
        return getEventType();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EventExternalized<?, ?> that && event.equals(that.event) && mapped.equals(that.mapped)
                && Objects.equals(brokerResult, that.brokerResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, mapped, brokerResult);
    }

    @Override
    public String toString() {
        return "EventExternalized[" + event.getClass().getName() + " to " + target + "]";
    }
}
