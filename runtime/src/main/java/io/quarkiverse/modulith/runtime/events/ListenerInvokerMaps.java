package io.quarkiverse.modulith.runtime.events;

import java.util.Map;
import java.util.function.Predicate;

import jakarta.enterprise.invoke.Invoker;

/**
 * The {@link ListenerInvokers} the generated bean creator builds.
 *
 * @param invokers the invoker of each listener, by listener identifier
 * @param conditions the compiled condition of each listener that has one, by listener identifier
 */
public record ListenerInvokerMaps(Map<String, Invoker<Object, Object>> invokers,
        Map<String, Predicate<Object>> conditions) implements ListenerInvokers {

    public ListenerInvokerMaps {
        invokers = Map.copyOf(invokers);
        conditions = Map.copyOf(conditions);
    }
}
