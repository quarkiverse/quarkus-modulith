package io.quarkiverse.modulith.core.data;

/**
 * A method the framework calls into a module.
 *
 * @param type the name of the type declaring the method
 * @param method the method name
 * @param kind the name of the {@code EntryPointKind}
 * @param detail what triggers the method: the HTTP method and path, the channel, the schedule or the event type
 */
public record EntryPointData(String type, String method, String kind, String detail) {
}
