package io.quarkiverse.modulith.junit.diff;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A file modified in the Git working tree or between two commits, with its path relative to the repository root or,
 * once rebased by the change detection, to the current working directory. Paths always use {@code /} as separator.
 *
 * @param path the path of the file, never {@code null}
 */
public record ModifiedFile(String path) {

    /**
     * @return whether the modified file is a Java source file
     */
    public boolean isJavaSource() {
        return "java".equalsIgnoreCase(extension());
    }

    /**
     * @return whether the modified file is a Kotlin source file
     */
    public boolean isKotlinSource() {
        return "kt".equalsIgnoreCase(extension());
    }

    /**
     * Creates a {@link ModifiedFile} for each of the given paths.
     *
     * @param paths the paths of the modified files
     * @return the modified files, never {@code null}
     */
    public static Stream<ModifiedFile> of(String... paths) {
        return Arrays.stream(paths).map(ModifiedFile::new);
    }

    /**
     * @param reference a repository-relative directory, the empty string for the repository root
     * @return whether the file is located in the given directory or one of its sub directories
     */
    boolean isLocatedIn(String reference) {
        return reference.isEmpty() || path.startsWith(reference + "/");
    }

    /**
     * Returns the file relative to the given reference directory, so {@code foo/bar.txt} relative to {@code foo} is
     * {@code bar.txt}.
     *
     * @param reference a repository-relative directory, the empty string for the repository root
     * @return the rebased file, never {@code null}
     * @throws IllegalArgumentException if the file is not located in the reference directory
     */
    ModifiedFile asRelativeTo(String reference) {
        if (!isLocatedIn(reference)) {
            throw new IllegalArgumentException("Modified file %s is not located in %s!".formatted(path, reference));
        }
        return reference.isEmpty() ? this : new ModifiedFile(path.substring(reference.length() + 1));
    }

    private String extension() {
        var dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(dot + 1) : "";
    }
}
