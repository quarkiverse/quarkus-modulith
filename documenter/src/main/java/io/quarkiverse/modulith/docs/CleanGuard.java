package io.quarkiverse.modulith.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Decides whether the {@link Documenter} may empty an output folder, the counterpart of the build's rule that
 * {@code quarkus.modulith.docs.output-directory} lies strictly inside the build output. A plain test can write
 * anywhere, so the rule refuses what would delete sources: a file system root, the working directory or one of its
 * ancestors, a project folder, and inside the working directory anything but a sub-folder of {@code target} or
 * {@code build}.
 */
final class CleanGuard {

    private static final List<String> BUILD_FILES = List.of("pom.xml", "build.gradle", "build.gradle.kts");
    private static final List<String> BUILD_OUTPUTS = List.of("target", "build");

    private CleanGuard() {
    }

    /**
     * @param folder an existing folder about to be emptied
     * @param workingDirectory the working directory of the process
     * @throws IllegalArgumentException when emptying the folder could delete anything but generated files
     */
    static void requireCleanable(Path folder, Path workingDirectory) throws IOException {
        Path real = folder.toRealPath();
        Path work = workingDirectory.toAbsolutePath().toRealPath();
        if (Objects.isNull(real.getParent())) {
            throw refused(folder, "it is a file system root");
        }
        if (work.startsWith(real)) {
            throw refused(folder, "it is the working directory or one of its ancestors");
        }
        for (String buildFile : BUILD_FILES) {
            if (Files.exists(real.resolve(buildFile))) {
                throw refused(folder, "it holds a " + buildFile);
            }
        }
        if (real.startsWith(work) && BUILD_OUTPUTS.stream().map(work::resolve)
                .noneMatch(output -> real.startsWith(output) && !real.equals(output))) {
            throw refused(folder, "it lies in the working directory outside a sub-folder of target or build");
        }
    }

    private static IllegalArgumentException refused(Path folder, String reason) {
        return new IllegalArgumentException("Refusing to clean the documentation output " + folder + ": " + reason
                + ". Choose another output folder or use DocumenterOptions.withoutClean().");
    }
}
