package io.quarkiverse.modulith.events.externalization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The routing expressions of {@code Externalized} annotations, compiled at build time. The build generates a subclass
 * that registers every expression in its constructor. The expressions are frozen at the first lookup, so that the map
 * read by every sending thread is never written again.
 */
public abstract class CompiledRoutingExpressions {

    private final Map<String, Map<String, RoutingExpression>> registered = new HashMap<>();
    private volatile Map<String, Map<String, RoutingExpression>> frozen;

    /**
     * Registers the compiled form of an expression declared on a type. Only for the generated constructor, which
     * lives in another package and so cannot call a protected method.
     *
     * @throws IllegalStateException once the expressions were looked up
     */
    public void register(String declaringType, String expression, RoutingExpression compiled) {
        if (Objects.nonNull(frozen)) {
            throw new IllegalStateException("Routing expressions are registered by the generated constructor only");
        }
        registered.computeIfAbsent(declaringType, type -> new HashMap<>()).put(expression, compiled);
    }

    /**
     * @return the compiled expression declared on the given type
     */
    public Optional<RoutingExpression> find(String declaringType, String expression) {
        return Optional.ofNullable(expressions().getOrDefault(declaringType, Map.of()).get(expression));
    }

    private Map<String, Map<String, RoutingExpression>> expressions() {
        Map<String, Map<String, RoutingExpression>> current = frozen;
        if (Objects.isNull(current)) {
            current = registered.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
            frozen = current;
        }
        return current;
    }
}
