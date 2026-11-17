package io.quarkiverse.modulith.core.impl;

import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.lang.ArchRule;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violations;

/**
 * The entry to the ArchUnit rules from the verification. No ArchUnit type appears in its method signatures, so a native
 * image built without ArchUnit or jMolecules can replace these methods (see the runtime's {@code graal} package) and
 * never links the classes that need the libraries.
 */
public final class RuleSupport {

    private RuleSupport() {
    }

    /**
     * @return the jMolecules rules for the libraries on the class path, as ArchUnit {@code ArchRule} instances
     */
    public static List<Object> jMoleculesRules() {
        return List.copyOf(ArchUnitSupport.jMoleculesRules());
    }

    /**
     * @param rules ArchUnit {@code ArchRule} instances
     * @return one violation per failure detail
     */
    @SuppressWarnings("unchecked")
    public static Violations evaluate(Collection<?> rules, ApplicationModules modules) {
        return ArchUnitSupport.evaluate((Collection<ArchRule>) rules, modules);
    }
}
