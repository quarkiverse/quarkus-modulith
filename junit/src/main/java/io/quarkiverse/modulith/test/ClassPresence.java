package io.quarkiverse.modulith.test;

/**
 * Detects optional libraries.
 */
final class ClassPresence {

    static final String ASSERTJ = "org.assertj.core.api.AssertProvider";
    static final String NARAYANA = "io.quarkus.narayana.jta.QuarkusTransaction";

    private ClassPresence() {
    }

    /**
     * @return whether the class can be loaded by the loader of this class
     */
    static boolean isPresent(String className) {
        try {
            Class.forName(className, false, ClassPresence.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
