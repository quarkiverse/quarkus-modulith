package io.quarkiverse.modulith.deployment.model;

import java.util.List;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModulithRule;
import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.core.Violations;

/**
 * Reports the number of modules as a violation.
 */
public class CountingRule implements ModulithRule {

    @Override
    public Violations verify(ApplicationModules modules) {
        return new Violations(List.of(new Violation(Violation.Kind.ADDITIONAL_RULE, "", "", "",
                modules.stream().count() + " module(s)")));
    }
}
