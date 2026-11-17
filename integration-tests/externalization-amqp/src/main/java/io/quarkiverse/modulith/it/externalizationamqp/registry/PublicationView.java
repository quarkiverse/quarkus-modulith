package io.quarkiverse.modulith.it.externalizationamqp.registry;

import java.util.UUID;

import io.quarkiverse.modulith.events.core.TargetEventPublication;

public record PublicationView(UUID id, String status, String event, int completionAttempts) {

    static PublicationView of(TargetEventPublication publication) {
        return new PublicationView(publication.getIdentifier(), publication.getStatus().name(),
                publication.getEvent().toString(), publication.getCompletionAttempts());
    }
}
