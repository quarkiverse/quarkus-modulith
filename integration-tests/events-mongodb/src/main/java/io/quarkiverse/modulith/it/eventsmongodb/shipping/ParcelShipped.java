package io.quarkiverse.modulith.it.eventsmongodb.shipping;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The event the application stores, registered for reflection because the repository loads its class by name and
 * Jackson reads it, also in a native executable.
 *
 * @param label the label of the parcel
 */
@RegisterForReflection
public record ParcelShipped(String label) {
}
