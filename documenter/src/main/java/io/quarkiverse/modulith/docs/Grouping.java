package io.quarkiverse.modulith.docs;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A named group of beans on a module canvas, Spring Modulith's {@code CanvasOptions.Grouping}.
 */
public final class Grouping {

    private final String name;
    private final Predicate<ModuleBean> predicate;
    private final Optional<String> description;

    private Grouping(String name, Predicate<ModuleBean> predicate, Optional<String> description) {
        if (Objects.isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or empty!");
        }
        if (description.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException("Description must not be empty or null!");
        }
        this.name = name;
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.description = description;
    }

    public static Grouping of(String name, Predicate<ModuleBean> predicate) {
        return new Grouping(name, predicate, Optional.empty());
    }

    /**
     * @param description shown after the group name, {@code null} for none
     */
    public static Grouping of(String name, Predicate<ModuleBean> predicate, String description) {
        return new Grouping(name, predicate, Optional.ofNullable(description));
    }

    /**
     * @return matches beans whose fully qualified class name matches the regular expression
     * @throws java.util.regex.PatternSyntaxException when the pattern is not a valid regular expression
     */
    public static Predicate<ModuleBean> nameMatching(String pattern) {
        if (Objects.isNull(pattern) || pattern.isBlank()) {
            throw new IllegalArgumentException("Pattern must not be null or empty!");
        }
        Pattern compiled = Pattern.compile(pattern);
        return bean -> compiled.matcher(bean.type().name()).matches();
    }

    /**
     * @return matches beans assignable to the given type, the type itself included
     */
    public static Predicate<ModuleBean> implementing(Class<?> type) {
        return implementing(type.getName());
    }

    /**
     * Same as {@link #implementing(Class)}, by binary type name.
     */
    public static Predicate<ModuleBean> implementing(String type) {
        Objects.requireNonNull(type, "type");
        return bean -> bean.type().isAssignableTo(type);
    }

    /**
     * @return matches beans assignable to the given type, the type itself excluded
     */
    public static Predicate<ModuleBean> subtypeOf(Class<?> type) {
        return subtypeOf(type.getName());
    }

    /**
     * Same as {@link #subtypeOf(Class)}, by binary type name.
     */
    public static Predicate<ModuleBean> subtypeOf(String type) {
        return implementing(type).and(bean -> !bean.type().name().equals(type));
    }

    /**
     * @return matches beans whose class is annotated with the given annotation
     */
    public static Predicate<ModuleBean> isAnnotatedWith(Class<? extends Annotation> type) {
        return isAnnotatedWith(type.getName());
    }

    /**
     * Same as {@link #isAnnotatedWith(Class)}, by annotation name.
     */
    public static Predicate<ModuleBean> isAnnotatedWith(String annotation) {
        Objects.requireNonNull(annotation, "annotation");
        return bean -> bean.type().isAnnotatedWith(annotation);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public boolean matches(ModuleBean candidate) {
        return predicate.test(Objects.requireNonNull(candidate, "candidate"));
    }
}
