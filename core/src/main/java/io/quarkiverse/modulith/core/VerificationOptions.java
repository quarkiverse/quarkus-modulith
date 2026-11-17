package io.quarkiverse.modulith.core;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.tngtech.archunit.lang.ArchRule;

import io.quarkiverse.modulith.core.impl.ClassPresence;
import io.quarkiverse.modulith.core.impl.RuleSupport;

/**
 * The verifications {@link ApplicationModules#verify(VerificationOptions)} runs in addition to the module rules, like
 * Spring Modulith's {@code VerificationOptions}. ArchUnit rules need ArchUnit on the class path, which is the case in
 * tests. By default the jMolecules rules run when {@code jmolecules-archunit} is on the class path.
 */
public final class VerificationOptions {

    private final List<ArchRule> additionalVerifications;
    private final List<ModulithRule> rules;

    private VerificationOptions(Collection<ArchRule> additionalVerifications, Collection<ModulithRule> rules) {
        this.additionalVerifications = List.copyOf(additionalVerifications);
        this.rules = List.copyOf(rules);
    }

    /**
     * @return the jMolecules DDD, hexagonal (lenient), layered and onion rules for the architecture libraries on the
     *         class path, nothing without {@code jmolecules-archunit}
     */
    public static VerificationOptions defaults() {
        return defaults(ClassPresence.hasJMoleculesRules());
    }

    static VerificationOptions defaults(boolean jMolecules) {
        return new VerificationOptions(jMolecules ? archRules(RuleSupport.jMoleculesRules()) : List.of(), List.of());
    }

    /**
     * @return options with the given ArchUnit rules instead of the current ones
     */
    public VerificationOptions withAdditionalVerifications(Collection<ArchRule> verifications) {
        return new VerificationOptions(verifications, rules);
    }

    public VerificationOptions withAdditionalVerifications(ArchRule... verifications) {
        return withAdditionalVerifications(List.of(verifications));
    }

    /**
     * @return options with the given ArchUnit rules added to the current ones
     */
    public VerificationOptions andAdditionalVerifications(Collection<ArchRule> verifications) {
        return new VerificationOptions(Stream.concat(additionalVerifications.stream(), verifications.stream()).toList(),
                rules);
    }

    public VerificationOptions andAdditionalVerifications(ArchRule... verifications) {
        return andAdditionalVerifications(List.of(verifications));
    }

    /**
     * @return options without ArchUnit rules and without module rules
     */
    public VerificationOptions withoutAdditionalVerifications() {
        return new VerificationOptions(List.of(), List.of());
    }

    /**
     * @return options with the given module rules added to the current ones
     */
    public VerificationOptions andRules(ModulithRule... rules) {
        return new VerificationOptions(additionalVerifications,
                Stream.concat(this.rules.stream(), Stream.of(rules)).toList());
    }

    /**
     * @return options with the jMolecules rules added, as {@link #defaults()} has them
     * @throws IllegalStateException if {@code jmolecules-archunit} is not on the class path
     */
    public VerificationOptions andJMoleculesRules() {
        return andJMoleculesRules(ClassPresence.hasJMoleculesRules());
    }

    VerificationOptions andJMoleculesRules(boolean present) {
        if (!present) {
            throw new IllegalStateException("The jMolecules rules need org.jmolecules.integrations:jmolecules-archunit"
                    + " on the class path. Add it, or turn quarkus.modulith.verification.jmolecules-rules off.");
        }
        return andAdditionalVerifications(archRules(RuleSupport.jMoleculesRules()));
    }

    Collection<ArchRule> getAdditionalVerifications() {
        return additionalVerifications;
    }

    /**
     * @return the violations of the module rules and of the ArchUnit rules
     */
    Violations evaluate(ApplicationModules modules) {
        Violations result = rules.stream().map(it -> it.verify(modules)).reduce(Violations.NONE, Violations::and);
        return additionalVerifications.isEmpty() ? result
                : result.and(RuleSupport.evaluate(additionalVerifications, modules));
    }

    @SuppressWarnings("unchecked")
    private static List<ArchRule> archRules(List<Object> rules) {
        return (List<ArchRule>) (List<?>) rules;
    }
}
