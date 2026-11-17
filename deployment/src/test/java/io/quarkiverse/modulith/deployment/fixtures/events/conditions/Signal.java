package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

/**
 * Has methods the accessor search must skip: a static {@code getFlag()}, a void {@code isFlag()} and a package-private
 * {@code getName()}.
 */
public record Signal(boolean flag, String name, String[] tags) {

    public static String getFlag() {
        return "static";
    }

    public void isFlag() {
        // not an accessor
    }

    String getName() {
        return "hidden";
    }
}
