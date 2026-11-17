package io.quarkiverse.modulith.events.tck;

/**
 * The event the contract tests publish. A record with a plain string, so every serializer can store and read it.
 *
 * @param id an identifier that makes events distinguishable
 */
public record TckEvent(String id) {
}
