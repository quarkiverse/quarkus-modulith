package io.quarkiverse.modulith.core.data;

import java.util.List;

/**
 * A method of an event listener type that receives events.
 *
 * @param name the method name
 * @param parameterTypes the fully qualified names of the parameter types
 * @param async whether the method runs asynchronously
 * @param transactionPhase the transaction phase the method is bound to, empty if none
 * @param referenceTypes the event types the method is interested in
 */
public record ReferenceMethodData(String name, List<String> parameterTypes, boolean async, String transactionPhase,
        List<String> referenceTypes) {
}
