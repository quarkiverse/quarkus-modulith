package io.quarkiverse.modulith.core.impl;

import java.util.Optional;

/**
 * Checks whether optional libraries are on the class path without loading their classes.
 */
public final class ClassPresence {

    private ClassPresence() {
    }

    /**
     * @return whether the class file of the given class is on the context class path
     */
    public static boolean isPresent(String name) {
        return Optional.ofNullable(ApplicationModulesLoader.classLoader().getResource(name.replace('.', '/') + ".class"))
                .isPresent();
    }

    /**
     * @return whether the jMolecules ArchUnit rules can run
     */
    public static boolean hasJMoleculesRules() {
        return isPresent("org.jmolecules.archunit.JMoleculesArchitectureRules");
    }
}
