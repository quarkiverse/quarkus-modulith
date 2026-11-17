package io.quarkiverse.modulith.core.fixtures.scan;

public class Target {

    public Target() {
    }

    public static Target create() {
        return new Target();
    }

    public void run() {
    }

    public class Inner<T> {
    }

    protected static class Protected {
    }
}
