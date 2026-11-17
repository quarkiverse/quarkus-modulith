package io.quarkiverse.modulith.core;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A package expression with the wildcards {@code *} (any characters) and {@code ?} (one character), compiled once. All
 * other characters match literally. An expression without a dot matches a single segment of a package name, one with a
 * dot the whole name.
 */
final class PackageExpression {

    private static final String WILDCARD_BOUNDARY = "(?<=[*?])|(?=[*?])";

    private final Pattern pattern;
    private final boolean wholeName;

    private PackageExpression(Pattern pattern, boolean wholeName) {
        this.pattern = pattern;
        this.wholeName = wholeName;
    }

    static PackageExpression of(String expression) {
        String regex = Arrays.stream(expression.split(WILDCARD_BOUNDARY))
                .map(it -> switch (it) {
                    case "*" -> ".*";
                    case "?" -> ".";
                    default -> Pattern.quote(it);
                })
                .collect(Collectors.joining());
        return new PackageExpression(Pattern.compile(regex), expression.contains("."));
    }

    boolean matches(PackageName name) {
        return wholeName ? pattern.matcher(name.getName()).matches()
                : name.segments().anyMatch(it -> pattern.matcher(it).matches());
    }
}
