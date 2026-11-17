package io.quarkiverse.modulith.core;

import java.io.Serializable;

/**
 * A single breach of the module rules.
 *
 * @param kind the rule that was broken
 * @param module the module that breaks the rule
 * @param source the type, or the module cycle, where the breach was found
 * @param target the type or module that should not have been referenced, empty if not applicable
 * @param message a readable description, the same text Spring Modulith uses where it reports the same breach
 */
public record Violation(Kind kind, String module, String source, String target, String message) implements Serializable {

    /**
     * The rules. Nested in the record unlike the other types of the API, because {@code Violation.Kind} is published
     * API; it moves to a file of its own only with the next breaking release.
     */
    public enum Kind {

        /**
         * Closed modules form a dependency cycle.
         */
        CYCLE,

        /**
         * A type references a type of another module that is neither in its base package nor in a named interface.
         */
        NON_EXPOSED_TYPE,

        /**
         * A module depends on a module, or a named interface, that its {@code allowedDependencies} do not list.
         */
        DEPENDENCY_NOT_ALLOWED,

        /**
         * A module declaration is invalid, for example it names a module that does not exist.
         */
        INVALID_DECLARATION,

        /**
         * A type of a nested module is used from outside its parent module and the parent's other nested modules.
         */
        NESTED_MODULE_ACCESS,

        /**
         * A bean of another module is injected into a field instead of a constructor.
         */
        FIELD_INJECTION,

        /**
         * An additional rule, such as an ArchUnit or jMolecules rule, failed.
         */
        ADDITIONAL_RULE
    }

    /**
     * @return the message, under Spring Modulith's name
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return whether the message contains the given text
     */
    public boolean hasMessageContaining(String candidate) {
        return message.contains(candidate);
    }

    @Override
    public String toString() {
        return message;
    }
}
