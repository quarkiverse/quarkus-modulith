package io.quarkiverse.modulith.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Writes the documentation to a folder of the file system. Files stay inside the folder, and {@link #clear()} empties
 * it only when {@link CleanGuard} allows it.
 */
final class FolderOutput implements DocumentationOutput {

    private final Path folder;
    private final Path workingDirectory;

    FolderOutput(Path folder) {
        this(folder, Path.of(""));
    }

    /**
     * @param workingDirectory the folder {@link #clear()} protects as the project, the working directory outside tests
     */
    FolderOutput(Path folder, Path workingDirectory) {
        this.folder = folder;
        this.workingDirectory = workingDirectory;
    }

    Path folder() {
        return folder;
    }

    @Override
    public boolean contains(String name) {
        return Files.exists(folder.resolve(name));
    }

    @Override
    public void write(String name, String content) {
        Path file = folder.resolve(name);
        if (!file.normalize().startsWith(folder.normalize())) {
            throw new IllegalArgumentException("File name " + name + " leaves the documentation output " + folder);
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    @Override
    public void clear() {
        if (!Files.exists(folder)) {
            return;
        }
        try {
            CleanGuard.requireCleanable(folder, workingDirectory);
            deleteRecursively();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + folder, e);
        }
    }

    private void deleteRecursively() throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            List<Path> files = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : files) {
                Files.delete(path);
            }
        }
    }

    @Override
    public String toString() {
        return folder.toString();
    }
}
