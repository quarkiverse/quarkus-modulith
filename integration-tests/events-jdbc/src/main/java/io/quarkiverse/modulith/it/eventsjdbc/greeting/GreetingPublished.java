package io.quarkiverse.modulith.it.eventsjdbc.greeting;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The event the application stores. Registered for reflection because the repository loads its class by name and
 * Jackson reads it, also in a native executable.
 *
 * @param message the greeting
 */
@RegisterForReflection
public record GreetingPublished(String message) {
}
