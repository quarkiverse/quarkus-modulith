package io.quarkiverse.modulith.core;

/**
 * An additional verification over the module model. Unlike an ArchUnit rule it works on the recorded model, so it
 * also runs at build time: list implementations in {@code quarkus.modulith.verification.additional-rules}, or pass
 * them to {@link VerificationOptions#andRules(ModulithRule...)} in a test. Implementations need a public no-argument
 * constructor to be used from configuration.
 */
@FunctionalInterface
public interface ModulithRule {

    /**
     * @return the violations of the rule, {@link Violations#NONE} if there are none
     */
    Violations verify(ApplicationModules modules);
}
