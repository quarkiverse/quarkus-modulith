package io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile;

public class KeyedEvent implements Keyed<Labeled> {

    @Override
    public Labeled key() {
        return () -> "key";
    }
}
