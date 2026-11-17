package io.quarkiverse.modulith.runtime.graal;

import java.util.Collection;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violations;

/**
 * Without ArchUnit in the native image the evaluation of ArchUnit rules cannot be linked. Such rules need ArchUnit to
 * exist, so {@code detectViolations()} and {@code verify()} only run the module rules and never reach this method.
 */
@TargetClass(className = "io.quarkiverse.modulith.core.impl.RuleSupport", onlyWith = ArchUnitMissing.class)
final class Target_RuleSupport_WithoutArchUnit {

    private Target_RuleSupport_WithoutArchUnit() {
    }

    @Substitute
    public static Violations evaluate(Collection<?> rules, ApplicationModules modules) {
        throw new IllegalStateException("ArchUnit rules need com.tngtech.archunit:archunit, which is not part of the"
                + " native executable");
    }
}
