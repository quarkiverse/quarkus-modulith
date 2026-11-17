package io.quarkiverse.modulith.it.eventsneo4j.catalog;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The event the application stores, registered for reflection because the repository loads its class by name and
 * Jackson reads it, also in a native executable.
 *
 * @param sku the product
 */
@RegisterForReflection
public record ProductListed(String sku) {
}
