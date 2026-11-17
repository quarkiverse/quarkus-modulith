package io.quarkiverse.modulith.events.jobrunr.runtime;

import org.jobrunr.jobs.lambdas.JobRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The job of one externalized event for one {@link io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport}.
 * The event is stored as written by the {@code EventSerializer}, like an event publication, so that the JSON mapper of
 * JobRunr only sees strings. JobRunr writes the fields and reads them back through the constructor.
 */
public class ExternalizationJobRequest implements JobRequest {

    private String transport;
    private String eventType;
    private String event;

    /**
     * The creator of the JSON mappers JobRunr uses, Jackson 2 on Quarkus 3 and Jackson 3 on Quarkus 4.
     */
    @JsonCreator
    public ExternalizationJobRequest(@JsonProperty("transport") String transport,
            @JsonProperty("eventType") String eventType, @JsonProperty("event") String event) {
        this.transport = transport;
        this.eventType = eventType;
        this.event = event;
    }

    /**
     * @return the bean class of the transport that sends the event
     */
    public String getTransport() {
        return transport;
    }

    /**
     * @return the class of the event
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * @return the serialized event
     */
    public String getEvent() {
        return event;
    }

    @Override
    public Class<ExternalizationJobRequestHandler> getJobRequestHandler() {
        return ExternalizationJobRequestHandler.class;
    }
}
