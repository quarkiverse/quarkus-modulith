package io.quarkiverse.modulith.deployment.events;

import java.util.List;

/**
 * The event or a property path on it. A segment ending with {@code ()} calls a method without parameters, any other
 * segment reads a property through {@code getX()}, {@code isX()} or a record accessor {@code x()}.
 *
 * @param segments the segments after the event, empty for the event itself
 */
public record PathNode(List<String> segments) implements ConditionNode {

    public PathNode {
        segments = List.copyOf(segments);
    }
}
