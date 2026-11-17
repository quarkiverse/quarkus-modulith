package io.quarkiverse.modulith.events.deployment.fixtures;

public class Unwritable {

    public String getValue() {
        throw new IllegalStateException("cannot read");
    }
}
