package io.quarkiverse.modulith.observability.runtime;

import java.lang.reflect.Method;

/**
 * A method called on an instance of a type, the cache key of {@link ObservedModules}.
 */
record CallSite(Class<?> type, Method method) {
}
