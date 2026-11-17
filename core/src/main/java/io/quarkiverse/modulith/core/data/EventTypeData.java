package io.quarkiverse.modulith.core.data;

import java.util.List;

/**
 * An event type published by a module.
 *
 * @param type the name of the event type
 * @param sources the places outside the event type that create instances of it
 */
public record EventTypeData(String type, List<EventSourceData> sources) {
}
