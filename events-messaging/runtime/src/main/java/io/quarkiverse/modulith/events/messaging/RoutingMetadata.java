package io.quarkiverse.modulith.events.messaging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkiverse.modulith.events.RoutingTarget;

/**
 * Message metadata of every externalized event: where it goes and the headers computed for it. Connectors without
 * metadata of their own, and interceptors, read it, like the {@code springModulith_routingTarget} header of Spring
 * Modulith's Spring Messaging externalization.
 *
 * @param target the routing target, with expressions resolved
 * @param headers the headers of the externalization configuration, plus {@link #ROUTING_TARGET_HEADER} and
 *        {@link #SPRING_ROUTING_TARGET_HEADER}, in that order, without the headers whose value is {@code null}
 */
public record RoutingMetadata(RoutingTarget target, Map<String, Object> headers) {

    /**
     * The header that carries the routing target, written {@code target} or {@code target::key}.
     */
    public static final String ROUTING_TARGET_HEADER = "quarkusModulith_routingTarget";

    /**
     * The header name Spring Modulith uses for the routing target, for downstream components written for it.
     */
    public static final String SPRING_ROUTING_TARGET_HEADER = "springModulith_routingTarget";

    public RoutingMetadata {
        Objects.requireNonNull(target, "target");
        Map<String, Object> present = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (Objects.nonNull(value)) {
                present.put(name, value);
            }
        });
        headers = Collections.unmodifiableMap(present);
    }

    /**
     * @return the metadata of an event sent to the target with the given headers, plus the routing target headers
     */
    public static RoutingMetadata of(RoutingTarget target, Map<String, Object> headers) {
        Map<String, Object> all = new LinkedHashMap<>(headers);
        all.put(ROUTING_TARGET_HEADER, target.toString());
        all.put(SPRING_ROUTING_TARGET_HEADER, target.toString());
        return new RoutingMetadata(target, all);
    }
}
