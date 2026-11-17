package io.quarkiverse.modulith.runtime.events;

import jakarta.transaction.Transactional.TxType;

/**
 * A method annotated with {@code @ApplicationModuleListener} (directly or through a composed annotation), as found at
 * build time.
 *
 * @param id the listener identifier stored with event publications
 * @param declaringClass the bean class declaring the method
 * @param method the method name
 * @param eventType the fully qualified name of the erasure of the event parameter type
 * @param module the module that declares the listener, empty when the listener is outside any module
 * @param async whether the listener runs on a worker thread
 * @param invokerClass the name of the ArC invoker generated for the method
 * @param observedType the event parameter type with its type arguments, the key of the CDI observer
 * @param condition the condition expression, empty when the listener always applies
 * @param propagation the transaction propagation the listener runs with
 * @param readOnly whether the listener transaction is read-only
 */
public record ListenerData(String id, String declaringClass, String method, String eventType, String module,
        boolean async, String invokerClass, String observedType, String condition, TxType propagation,
        boolean readOnly) {
}
