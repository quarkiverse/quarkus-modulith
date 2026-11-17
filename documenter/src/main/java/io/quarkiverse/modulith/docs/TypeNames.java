package io.quarkiverse.modulith.docs;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Formats type names like Spring Modulith's {@code FormattableType}: {@code com.acme.order.Order} becomes
 * {@code c.a.o.Order}, and types of a module are abbreviated relative to the module's base package. Public for the
 * other Quarkus Modulith extensions that render type names the same way.
 */
public final class TypeNames {

    private TypeNames() {
    }

    public static String packageOf(String type) {
        int dot = type.lastIndexOf('.');
        return dot < 0 ? "" : type.substring(0, dot);
    }

    /**
     * @return the name without package and enclosing types
     */
    public static String simpleName(String type) {
        return type.substring(Math.max(type.lastIndexOf('.'), type.lastIndexOf('$')) + 1);
    }

    public static String shortName(String type) {
        return type.substring(type.lastIndexOf('.') + 1).replace('$', '.');
    }

    /**
     * @return the name with every package segment cut to its first letter
     */
    public static String abbreviate(String type) {
        String packageName = packageOf(type);
        return packageName.isEmpty() ? shortName(type) : abbreviatePackage(packageName) + "." + shortName(type);
    }

    /**
     * @param basePackage the base package of the module the type belongs to, {@code null} for types outside modules
     * @return the abbreviated name, or the full name for a type outside the given base package
     */
    public static String abbreviate(String type, String basePackage) {
        return abbreviate(type, basePackage, false);
    }

    /**
     * @param abbreviateForeign whether types outside the base package are abbreviated too, instead of written in full
     * @return the abbreviated name, Spring Modulith's {@code FormattableType.getAbbreviatedFullName(module, ...)}. As
     *         there, the base package is matched as a plain prefix: {@code com.acme.orderly.Thing} counts as inside
     *         {@code com.acme.order}. The callers pass the base package of the type's own module.
     */
    public static String abbreviate(String type, String basePackage, boolean abbreviateForeign) {
        if (Objects.isNull(basePackage) || basePackage.isEmpty()) {
            return abbreviate(type);
        }
        String packageName = packageOf(type);
        if (basePackage.equals(packageName)) {
            return abbreviate(type);
        }
        if (!packageName.startsWith(basePackage)) {
            return abbreviateForeign ? abbreviate(type) : type.replace('$', '.');
        }
        return abbreviatePackage(basePackage) + packageName.substring(basePackage.length()) + "." + shortName(type);
    }

    private static String abbreviatePackage(String packageName) {
        return Arrays.stream(packageName.split("\\.")).map(it -> it.substring(0, 1)).collect(Collectors.joining("."));
    }
}
