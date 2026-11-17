package io.quarkiverse.modulith.docs;

/**
 * A method that creates an event.
 *
 * @param type the binary name of the type declaring the method
 * @param method the method name
 * @param parameters whether the method has parameters
 */
public record EventSource(String type, String method, boolean parameters) {
}
