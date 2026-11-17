package io.quarkiverse.modulith.junit;

import java.util.List;

/**
 * A change to a file that is neither Java nor Kotlin source.
 *
 * @param path the path of the file relative to the working directory
 */
record OtherFileChange(String path) implements Change {

    private static final List<String> CLASSPATH_RESOURCES = List.of("src/main/resources", "src/test/resources");

    /**
     * Patterns of files whose change forces a full test run because they influence how the module is built or tested,
     * in Spring Modulith's order. A leading {@code *} matches any file name at the module root. Paths are relative to
     * the working directory, so build files of nested modules or of {@code buildSrc/} do not match.
     */
    private static final List<String> BUILD_RESOURCE_PATTERNS = List.of(
            "pom.xml",
            "gradle.properties",
            "*.gradle",
            "*.gradle.kts",
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.properties");

    /**
     * @return whether the change affects a build resource
     */
    boolean affectsBuildResource() {
        return BUILD_RESOURCE_PATTERNS.stream().anyMatch(this::matches);
    }

    /**
     * @return whether the change affects a class path resource, in contrast to other files
     */
    boolean isClasspathResource() {
        return CLASSPATH_RESOURCES.stream().anyMatch(path::startsWith);
    }

    @Override
    public boolean hasOrigin(String nameOrPath) {
        return path.equals(nameOrPath);
    }

    private boolean matches(String pattern) {
        return pattern.startsWith("*")
                ? path.indexOf('/') < 0 && path.endsWith(pattern.substring(1))
                : pattern.equals(path);
    }
}
