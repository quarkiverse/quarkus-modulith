package io.quarkiverse.modulith.runtime.dev;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.IncompleteEventPublications;

/**
 * Dev UI backend of the incomplete publications page: lists them and submits them again. Registered only in dev mode
 * and only when the application has an event publication registry.
 */
public class ModulithJsonRpcService {

    /**
     * The most publications the page lists, so a large backlog does not freeze the browser.
     */
    static final int MAX_LISTED = 100;

    @Inject
    IncompleteEventPublications publications;

    /**
     * @return the oldest {@link #MAX_LISTED} incomplete publications, oldest first, without their payloads
     */
    public List<Map<String, Object>> getIncompletePublications() {
        return publications.findIncompletePublications().stream().sorted().limit(MAX_LISTED)
                .map(ModulithJsonRpcService::toMap).toList();
    }

    /**
     * @return the number of publications submitted again: 1, or 0 when the publication completed meanwhile
     */
    public int resubmit(String id) {
        return publications.resubmitIncompletePublications(it -> it.getIdentifier().toString().equals(id));
    }

    /**
     * @return the number of publications submitted again
     */
    public int resubmitAll() {
        return publications.resubmitIncompletePublications(it -> true);
    }

    static Map<String, Object> toMap(EventPublication publication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", publication.getIdentifier().toString());
        result.put("eventType", publication.getEventType());
        result.put("listenerId", publication.getListenerId());
        result.put("publicationDate", publication.getPublicationDate().toString());
        result.put("status", publication.getStatus().name());
        result.put("completionAttempts", publication.getCompletionAttempts());
        result.put("lastResubmissionDate", publication.getLastResubmissionDate().map(Object::toString).orElse(""));
        return result;
    }
}
