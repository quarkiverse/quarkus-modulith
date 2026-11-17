package io.quarkiverse.modulith.runtime.events;

import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.invoke.Invoker;

/**
 * A module listener ready to be called.
 *
 * @param data what the build found
 * @param invoker calls the listener method on its bean
 * @param condition the compiled condition, empty when the listener always applies
 */
record Listener(ListenerData data, Invoker<Object, Object> invoker, Optional<Predicate<Object>> condition) {
}
