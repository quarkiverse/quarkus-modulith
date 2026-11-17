package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesArchitectureRules.VerificationDepth;
import org.jmolecules.archunit.JMoleculesDddRules;

import com.tngtech.archunit.lang.ArchRule;

/**
 * The jMolecules rules Spring Modulith runs by default: DDD rules when the jMolecules DDD annotations are present,
 * hexagonal (lenient), layered and onion rules when the matching architecture library is present.
 */
final class JMoleculesRules {

    private JMoleculesRules() {
    }

    /**
     * Call only when {@link ClassPresence#hasJMoleculesRules()}.
     *
     * @param present tells whether a class is on the class path
     */
    static List<ArchRule> rules(Predicate<String> present) {
        List<ArchRule> rules = new ArrayList<>();
        if (present.test("org.jmolecules.ddd.annotation.Entity")) {
            rules.add(JMoleculesDddRules.all());
        }
        if (present.test("org.jmolecules.architecture.hexagonal.Port")) {
            rules.add(JMoleculesArchitectureRules.ensureHexagonal(VerificationDepth.LENIENT));
        }
        if (present.test("org.jmolecules.architecture.layered.InfrastructureLayer")) {
            rules.add(JMoleculesArchitectureRules.ensureLayering());
        }
        if (present.test("org.jmolecules.architecture.onion.classical.InfrastructureRing")) {
            rules.add(JMoleculesArchitectureRules.ensureOnionClassical());
            rules.add(JMoleculesArchitectureRules.ensureOnionSimple());
        }
        return rules;
    }
}
