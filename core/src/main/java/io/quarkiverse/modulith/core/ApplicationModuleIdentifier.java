package io.quarkiverse.modulith.core;

import java.util.Objects;

/**
 * The identifier of an application module, by default the last segment of its base package.
 *
 * @param value the identifier, never blank
 */
public record ApplicationModuleIdentifier(String value) implements Comparable<ApplicationModuleIdentifier> {

    public ApplicationModuleIdentifier {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalArgumentException("Module identifier must not be blank");
        }
    }

    public static ApplicationModuleIdentifier of(String value) {
        return new ApplicationModuleIdentifier(value);
    }

    @Override
    public int compareTo(ApplicationModuleIdentifier other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
