package io.quarkiverse.modulith.events.fixtures.app.inventory;

@io.quarkiverse.modulith.events.Externalized("quarkus-target")
@org.jmolecules.event.annotation.Externalized("jmolecules-target")
public record BothAnnotations(String sku) {
}
