package io.quarkiverse.modulith.core.data;

/**
 * A place in the code that creates an event.
 *
 * @param type the name of the type that creates the event
 * @param method the name of the method that creates the event, the enclosing method for lambdas
 * @param parameters whether that method declares parameters
 */
public record EventSourceData(String type, String method, boolean parameters) {
}
