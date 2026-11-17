package io.quarkiverse.modulith.events.jobrunr;

/**
 * Sends an externalized event from a JobRunr job, like Spring Modulith's {@code JobRunrExternalizationTransport}. In
 * the outbox externalization mode, every event the {@code EventExternalizationConfiguration} supports becomes one JobRunr
 * job per bean of this type, enqueued in the publishing transaction. A job that throws is retried by JobRunr.
 * <p>
 * When a broker transport such as {@code quarkus-modulith-events-jms} is present, a bean of this type that sends the
 * event through it is registered too. Applications declare their own beans for anything else. Each bean needs its own
 * bean class, because the jobs name the transport by it.
 */
@FunctionalInterface
public interface JobRunrExternalizationTransport {

    /**
     * Sends the event. Runs on a JobRunr worker thread, outside the publishing transaction.
     *
     * @param event the event as published, read back from the job
     */
    void externalize(Object event);
}
