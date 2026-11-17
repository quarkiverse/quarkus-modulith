package io.quarkiverse.modulith.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The kind of a reference from one module to another, like Spring Modulith's {@code DependencyType}.
 */
public enum DependencyType {

    /**
     * A bean of one module injects a bean of another, so the other module must be started too.
     */
    USES_COMPONENT,

    /**
     * A type of one module refers to an entity of another.
     */
    ENTITY,

    /**
     * A listener of one module consumes an event of another. The other module need not be started.
     */
    EVENT_LISTENER,

    /**
     * Any other reference.
     */
    DEFAULT;

    /**
     * @return every type except the given ones
     */
    public static Set<DependencyType> allBut(DependencyType... types) {
        EnumSet<DependencyType> excluded = EnumSet.noneOf(DependencyType.class);
        excluded.addAll(List.of(types));
        return EnumSet.complementOf(excluded);
    }

    /**
     * @return a description of a dependency of this type between the given types
     */
    public String format(FormattableType source, FormattableType target) {
        String from = source.getAbbreviatedFullName();
        String to = target.getAbbreviatedFullName();
        return switch (this) {
            case USES_COMPONENT -> "Component %s using %s".formatted(from, to);
            case ENTITY -> "Entity %s depending on %s".formatted(from, to);
            case EVENT_LISTENER -> "%s listening to events of type %s".formatted(from, to);
            case DEFAULT -> "%s depending on %s".formatted(from, to);
        };
    }
}
