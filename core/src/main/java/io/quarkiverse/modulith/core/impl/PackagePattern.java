package io.quarkiverse.modulith.core.impl;

import java.util.regex.Pattern;

/**
 * Matches package names against ArchUnit-style patterns: {@code a.b} is the package itself, {@code a.b..} the package
 * and its sub-packages, {@code ..b..} any package with a segment {@code b}, {@code *} any part of a segment.
 */
final class PackagePattern {

    private final Pattern pattern;

    private PackagePattern(Pattern pattern) {
        this.pattern = pattern;
    }

    static PackagePattern of(String expression) {
        String regex = expression.replace("*", "\\w*")
                .replace("..", "\u0000")
                .replace(".", "\\.");
        regex = regex.startsWith("\u0000") ? "(?:.*\\.)?" + regex.substring(1) : regex;
        regex = regex.endsWith("\u0000") ? regex.substring(0, regex.length() - 1) + "(?:\\..*)?" : regex;
        return new PackagePattern(Pattern.compile(regex.replace("\u0000", "\\.(?:.*\\.)?")));
    }

    boolean matches(String packageName) {
        return pattern.matcher(packageName).matches();
    }
}
