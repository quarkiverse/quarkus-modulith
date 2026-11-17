package io.quarkiverse.modulith.events.tck;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The in-memory repository has no rows, so a row without status is stored as what the contract says it reads as.
 */
class InMemoryMissingStatusTest extends MissingStatusContract {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    @Override
    protected UUID insertWithoutStatus(String eventId, Optional<Instant> completionDate) {
        UUID id = UUID.randomUUID();
        repository.create(TargetEventPublication.of(id, new TckEvent(eventId), TckEvent.class.getName(), LISTENER, T0,
                completionDate, completionDate.isPresent() ? Status.COMPLETED : Status.FAILED, 0, Optional.empty()));
        return id;
    }
}
