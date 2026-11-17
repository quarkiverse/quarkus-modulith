package io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan;

@io.quarkiverse.modulith.events.Externalized("quarkus")
@org.jmolecules.event.annotation.Externalized(target = "jmolecules")
public record Molecule(String id) {
}
