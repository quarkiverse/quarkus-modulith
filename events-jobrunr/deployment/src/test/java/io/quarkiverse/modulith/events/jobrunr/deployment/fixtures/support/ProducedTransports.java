package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

@Singleton
public class ProducedTransports {

    final List<Object> delivered = new CopyOnWriteArrayList<>();

    @Produces
    @Singleton
    JobRunrExternalizationTransport transport = delivered::add;

    public List<Object> delivered() {
        return List.copyOf(delivered);
    }
}
