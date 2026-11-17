package io.quarkiverse.modulith.it.externalizationjobrunr.delivery;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

/**
 * A transport of the application, called by JobRunr for every externalized event.
 */
@Singleton
public class WarehouseTransport implements JobRunrExternalizationTransport {

    final Recorder recorder = new Recorder();

    @Override
    public void externalize(Object event) {
        recorder.record("warehouse", event);
    }
}
