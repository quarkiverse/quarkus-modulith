package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

/**
 * Runs the externalization jobs: reads the event back and hands it to the transport the job names. JobRunr retries the
 * job when the transport throws.
 */
@Singleton
public class ExternalizationJobRequestHandler implements JobRequestHandler<ExternalizationJobRequest> {

    private static final Logger LOG = Logger.getLogger(ExternalizationJobRequestHandler.class);

    @Inject
    @Any
    Instance<JobRunrExternalizationTransport> transports;

    @Inject
    EventSerializer serializer;

    Map<String, JobRunrExternalizationTransport> byName;

    @PostConstruct
    void init() {
        byName = JobRunrTransports.byName(transports);
    }

    @Override
    public void run(ExternalizationJobRequest request) throws ClassNotFoundException {
        JobRunrExternalizationTransport transport = Optional.ofNullable(byName.get(request.getTransport()))
                .orElseThrow(() -> new IllegalStateException("The job names the JobRunrExternalizationTransport "
                        + request.getTransport() + ", which is not a bean of this application: add it back, or"
                        + " delete the job in the JobRunr dashboard"));
        Class<?> type = Class.forName(request.getEventType(), true, Thread.currentThread().getContextClassLoader());
        LOG.debugf("Externalizing an event of type %s through %s", request.getEventType(), request.getTransport());
        transport.externalize(serializer.deserialize(request.getEvent(), type));
    }
}
