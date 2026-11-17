package io.quarkiverse.modulith.events.registry;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.AbandonedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;

/**
 * The {@link AbandonedEventPublications} bean, backed by the registry.
 */
@Singleton
public class DefaultAbandonedEventPublications implements AbandonedEventPublications {

    @Inject
    DefaultEventPublicationRegistry registry;

    @Override
    public List<EventPublication> findAll() {
        return registry.findAbandoned();
    }

    @Override
    public int deletePublications(Predicate<EventPublication> filter) {
        return registry.deleteAbandoned(filter);
    }
}
