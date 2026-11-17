package io.quarkiverse.modulith.runtime.graal;

/**
 * Tells at native image build time whether an optional library is missing.
 */
final class MissingClass {

    private MissingClass() {
    }

    static boolean isMissing(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
