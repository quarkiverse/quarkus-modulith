package io.quarkiverse.modulith.docs;

import java.util.List;

/**
 * An event published by a module.
 *
 * @param type the event type
 * @param sources the methods creating the event, possibly none
 */
public record PublishedEvent(ModuleType type, List<EventSource> sources) {

    public PublishedEvent {
        sources = List.copyOf(sources);
    }
}
