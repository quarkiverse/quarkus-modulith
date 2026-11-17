package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;

@Singleton
public class Jobs {

    @Inject
    StorageProvider storage;

    public long count(StateName state) {
        return storage.countJobs(state);
    }

    public long total() {
        return Arrays.stream(StateName.values()).mapToLong(storage::countJobs).sum();
    }
}
