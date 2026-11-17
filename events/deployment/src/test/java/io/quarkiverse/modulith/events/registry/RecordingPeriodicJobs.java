package io.quarkiverse.modulith.events.registry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class RecordingPeriodicJobs implements PeriodicJobs {

    final Map<String, Duration> intervals = new LinkedHashMap<>();
    final Map<String, Runnable> tasks = new LinkedHashMap<>();

    @Override
    public void schedule(String identity, Duration interval, Runnable task) {
        intervals.put(identity, interval);
        tasks.put(identity, task);
    }
}
