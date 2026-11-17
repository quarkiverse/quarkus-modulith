package io.quarkiverse.modulith.junit.diff;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Restricts the files reported by a delegate to those located in the current, repository-relative working directory
 * and rebases their paths onto it, so change-aware execution is a per-module concept in multi-module builds.
 *
 * @param delegate the detector whose files to filter
 * @param workingDirectory the repository-relative working directory, the empty string for the repository root
 */
record WorkingDirectoryChangesDetector(FileModificationDetector delegate, String workingDirectory)
        implements
            FileModificationDetector {

    /**
     * @param delegate the detector whose files to filter
     * @param workingDirectory the absolute working directory
     * @return the detector for the working directory, empty outside of a Git repository
     * @throws UncheckedIOException if the working directory does not exist
     */
    static Optional<FileModificationDetector> of(FileModificationDetector delegate, Path workingDirectory) {
        var directory = workingDirectory.toAbsolutePath().normalize();
        return JGitUtil.findWorkTree(directory)
                .map(root -> new WorkingDirectoryChangesDetector(delegate, relativize(root, directory)));
    }

    @Override
    public Stream<ModifiedFile> getModifiedFiles() {
        return delegate.getModifiedFiles()
                .filter(it -> it.isLocatedIn(workingDirectory))
                .map(it -> it.asRelativeTo(workingDirectory));
    }

    private static String relativize(Path root, Path directory) {
        try {
            return StreamSupport.stream(root.toRealPath().relativize(directory.toRealPath()).spliterator(), false)
                    .map(Path::toString)
                    .collect(Collectors.joining("/"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
