package io.quarkiverse.modulith.events.deployment.externalization.fixtures.compile.other;

public abstract class BaseWithHidden {

    String hidden() {
        return "hidden";
    }

    public Internal internal() {
        return new Internal();
    }
}
