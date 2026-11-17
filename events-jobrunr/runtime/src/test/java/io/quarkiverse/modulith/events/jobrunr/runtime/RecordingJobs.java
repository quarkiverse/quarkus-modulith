package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingJobs extends JobRunrExternalizationJobs {

    final List<Object> enqueued = new CopyOnWriteArrayList<>();

    @Override
    public void enqueue(Object event) {
        enqueued.add(event);
    }
}
