package io.quarkiverse.modulith.runtime.dev;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.ResubmissionOptions;

/**
 * Incomplete publications held in memory; resubmitting counts the matching ones.
 */
record RecordingPublications(List<EventPublication> publications) implements IncompleteEventPublications {

    @Override
    public List<EventPublication> findIncompletePublications() {
        return publications;
    }

    @Override
    public int resubmitIncompletePublications(Predicate<EventPublication> filter) {
        return (int) publications.stream().filter(filter).count();
    }

    @Override
    public int resubmitIncompletePublicationsOlderThan(Duration duration) {
        return 0;
    }

    @Override
    public int resubmitIncompletePublications(ResubmissionOptions options) {
        return 0;
    }
}
