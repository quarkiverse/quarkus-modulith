package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.inject.Instance;

import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

/**
 * Names the {@link JobRunrExternalizationTransport} beans by their bean class, the name the jobs store. The build
 * fails when two transport beans share a bean class.
 */
final class JobRunrTransports {

    private JobRunrTransports() {
    }

    /**
     * @return the names of the transports, sorted
     */
    static List<String> names(Instance<JobRunrExternalizationTransport> transports) {
        return transports.handlesStream().map(handle -> handle.getBean().getBeanClass().getName()).sorted().toList();
    }

    /**
     * @return the transports by the name of their bean class, sorted by name
     */
    static Map<String, JobRunrExternalizationTransport> byName(Instance<JobRunrExternalizationTransport> transports) {
        Map<String, JobRunrExternalizationTransport> byName = new TreeMap<>();
        transports.handlesStream().forEach(handle -> byName.put(handle.getBean().getBeanClass().getName(), handle.get()));
        return byName;
    }
}
