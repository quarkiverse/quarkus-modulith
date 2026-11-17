package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.impl.CompiledRoutingExpressions;
import io.quarkiverse.modulith.events.externalization.impl.RoutingExpression;

/**
 * Resolves the expressions of a routing target against the original event, like Spring Modulith's
 * {@code BrokerRouting}. Expressions are compiled at build time from the {@code Externalized} annotations and found by
 * the type that declares them, so that a mapping applied later cannot change the routing.
 */
@Singleton
public class RoutingResolver {

    @Inject
    CompiledRoutingExpressions expressions;

    /**
     * The expression found for each event class, so that the hierarchy is walked once per class and expression.
     */
    final Map<Class<?>, Map<String, Optional<RoutingExpression>>> found = new ConcurrentHashMap<>();

    /**
     * @return the target with its expressions evaluated, the target itself when it has none
     * @throws IllegalStateException if an expression was not compiled or the target evaluates to {@code null}
     */
    public RoutingTarget resolve(RoutingTarget target, Object event) {
        if (!target.hasExpression()) {
            return target;
        }
        String resolvedTarget = evaluate(target.getTarget(), event);
        if (Objects.isNull(resolvedTarget) || resolvedTarget.isBlank()) {
            throw new IllegalStateException("Evaluation of target expression " + target.getTarget()
                    + " must not result in null or blank for an event of type " + event.getClass().getName());
        }
        Optional<String> key = target.getKey().map(declared -> evaluate(declared, event));
        return RoutingTarget.forTarget(resolvedTarget).withKey(key.orElse(null));
    }

    private String evaluate(String part, Object event) {
        if (!RoutingTarget.isExpression(part)) {
            return part;
        }
        return compiled(event.getClass(), part)
                .orElseThrow(() -> new IllegalStateException("Routing expression " + part + " for an event of type "
                        + event.getClass().getName() + " was not compiled at build time. Only expressions declared"
                        + " in @Externalized annotations are compiled"))
                .evaluate(event);
    }

    private Optional<RoutingExpression> compiled(Class<?> type, String expression) {
        return found.computeIfAbsent(type, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(expression, key -> lookUp(type, expression));
    }

    private Optional<RoutingExpression> lookUp(Class<?> type, String expression) {
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>(List.of(type));
        while (!pending.isEmpty()) {
            Class<?> current = pending.poll();
            if (!visited.add(current)) {
                continue;
            }
            Optional<RoutingExpression> found = expressions.find(current.getName(), expression);
            if (found.isPresent()) {
                return found;
            }
            Optional.ofNullable(current.getSuperclass()).ifPresent(pending::add);
            pending.addAll(List.of(current.getInterfaces()));
        }
        return Optional.empty();
    }
}
