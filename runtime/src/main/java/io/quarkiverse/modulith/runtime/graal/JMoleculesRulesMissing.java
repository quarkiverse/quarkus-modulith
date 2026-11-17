package io.quarkiverse.modulith.runtime.graal;

import java.util.function.BooleanSupplier;

/**
 * Holds when the jMolecules ArchUnit rules, an optional dependency of the core, are not part of the native image.
 */
public final class JMoleculesRulesMissing implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return MissingClass.isMissing("org.jmolecules.archunit.JMoleculesArchitectureRules",
                JMoleculesRulesMissing.class.getClassLoader());
    }
}
