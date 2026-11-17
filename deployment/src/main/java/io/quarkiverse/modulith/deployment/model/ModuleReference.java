package io.quarkiverse.modulith.deployment.model;

import java.util.Comparator;

/**
 * A reference from a type of one module to a type of another.
 *
 * @param source the referencing type
 * @param target the referenced type
 */
public record ModuleReference(String source, String target) implements Comparable<ModuleReference> {

    private static final Comparator<ModuleReference> ORDER = Comparator.comparing(ModuleReference::source)
            .thenComparing(ModuleReference::target);

    @Override
    public int compareTo(ModuleReference other) {
        return ORDER.compare(this, other);
    }
}
