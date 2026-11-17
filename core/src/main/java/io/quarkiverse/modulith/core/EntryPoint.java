package io.quarkiverse.modulith.core;

import io.quarkiverse.modulith.core.data.EntryPointData;

/**
 * A method the framework calls into a module, like the entry points Spring Modulith 2.2 lists per module.
 *
 * @param type the type declaring the method
 * @param method the method name
 * @param kind how the method is triggered
 * @param detail what triggers it: the HTTP method and path, the channel, the schedule or the event type
 */
public record EntryPoint(String type, String method, EntryPointKind kind, String detail) {

    static EntryPoint of(EntryPointData data) {
        return new EntryPoint(data.type(), data.method(), EntryPointKind.valueOf(data.kind()), data.detail());
    }
}
