package io.quarkiverse.modulith.runtime.events;

import java.util.Map;
import java.util.function.Predicate;

import jakarta.enterprise.invoke.Invoker;

/**
 * The invokers and compiled conditions of the module listeners. The deployment module generates the only
 * implementation, a bean that creates every invoker and condition with a plain constructor call, so no reflection is
 * needed.
 */
public interface ListenerInvokers {

    /**
     * @return the invoker of each listener, by listener identifier
     */
    Map<String, Invoker<Object, Object>> invokers();

    /**
     * @return the compiled condition of each listener that declares one, by listener identifier
     */
    Map<String, Predicate<Object>> conditions();
}
