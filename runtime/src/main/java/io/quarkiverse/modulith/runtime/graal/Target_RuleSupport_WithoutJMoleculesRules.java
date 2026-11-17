package io.quarkiverse.modulith.runtime.graal;

import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Without the jMolecules ArchUnit rules in the native image there are no jMolecules rules, as on the JVM.
 */
@TargetClass(className = "io.quarkiverse.modulith.core.impl.RuleSupport", onlyWith = JMoleculesRulesMissing.class)
final class Target_RuleSupport_WithoutJMoleculesRules {

    private Target_RuleSupport_WithoutJMoleculesRules() {
    }

    @Substitute
    public static List<Object> jMoleculesRules() {
        return List.of();
    }
}
