package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestScheduler;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

/**
 * Enqueues one JobRunr job per {@link JobRunrExternalizationTransport} for an event, like Spring Modulith's
 * {@code JobRunrEventExternalizer}. The jobs are saved in one batch through the storage provider, inside the current
 * transaction when there is one.
 */
@Singleton
public class JobRunrExternalizationJobs {

    @Inject
    JobRequestScheduler scheduler;

    @Inject
    EventSerializer serializer;

    @Inject
    @Any
    Instance<JobRunrExternalizationTransport> transports;

    List<String> names;

    @PostConstruct
    void init() {
        names = JobRunrTransports.names(transports);
    }

    /**
     * Saves the jobs of the event. Blocks while the jobs are written.
     */
    public void enqueue(Object event) {
        String type = event.getClass().getName();
        String serialized = serializer.serialize(event).toString();
        scheduler.create(names.stream().map(transport -> JobBuilder.aJob()
                .withName("Externalize " + event.getClass().getSimpleName() + " through "
                        + transport.substring(transport.lastIndexOf('.') + 1))
                .withJobRequest(new ExternalizationJobRequest(transport, type, serialized))));
    }
}
