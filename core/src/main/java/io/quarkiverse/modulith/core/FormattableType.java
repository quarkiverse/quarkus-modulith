package io.quarkiverse.modulith.core;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Renders type names for documentation and messages, abbreviating package names, like Spring Modulith's
 * {@code FormattableType}. Nested and array types are rendered with {@code Class#getTypeName()} semantics.
 */
public final class FormattableType {

    private final String type;

    private FormattableType(String type) {
        this.type = type;
    }

    public static FormattableType of(Class<?> type) {
        return new FormattableType(type.getTypeName());
    }

    public static FormattableType of(JavaType type) {
        return new FormattableType(type.getName());
    }

    /**
     * @param typeName a binary or canonical type name
     */
    public static FormattableType of(String typeName) {
        return new FormattableType(typeName);
    }

    /**
     * @return the abbreviated names of the given types, separated by commas
     */
    public static String format(Iterable<JavaType> types) {
        return StreamSupport.stream(types.spliterator(), false)
                .map(FormattableType::of)
                .map(FormattableType::getAbbreviatedFullName)
                .collect(Collectors.joining(", "));
    }

    /**
     * @return the name with each package segment shortened to its first letter, {@code c.a.o.Order}
     */
    public String getAbbreviatedFullName() {
        String packageName = packageName();
        return packageName.isEmpty() ? shortName() : abbreviate(packageName) + "." + shortName();
    }

    /**
     * @return the name relative to the module: types of the module base package abbreviated, types in sub-packages of
     *         the module with the base package abbreviated and the rest in full, types outside the module in full
     */
    public String getAbbreviatedFullName(ApplicationModule module) {
        return getAbbreviatedFullName(module, NonModuleTypeAbbreviation.FULL_NAME);
    }

    /**
     * @param module may be {@code null}, the result is then {@link #getAbbreviatedFullName()}
     */
    public String getAbbreviatedFullName(ApplicationModule module, NonModuleTypeAbbreviation abbreviation) {
        if (Objects.isNull(module) || module.getBasePackage().isEmpty()) {
            return getAbbreviatedFullName();
        }
        String basePackageName = module.getBasePackage();
        String typePackageName = packageName();
        if (basePackageName.equals(typePackageName)) {
            return getAbbreviatedFullName();
        }
        if (!PackageName.of(basePackageName).contains(PackageName.of(typePackageName))) {
            return abbreviation == NonModuleTypeAbbreviation.FULL_NAME ? getFullName() : getAbbreviatedFullName();
        }
        return abbreviate(basePackageName) + typePackageName.substring(basePackageName.length()) + "." + shortName();
    }

    /**
     * @return the full name with nested types separated by dots
     */
    public String getFullName() {
        return type.replace('$', '.');
    }

    private String packageName() {
        return PackageName.packageOf(type);
    }

    private String shortName() {
        return type.substring(type.lastIndexOf('.') + 1).replace('$', '.');
    }

    private static String abbreviate(String source) {
        return Stream.of(source.split("\\.")).map(it -> it.substring(0, 1)).collect(Collectors.joining("."));
    }

    @Override
    public String toString() {
        return type;
    }
}
