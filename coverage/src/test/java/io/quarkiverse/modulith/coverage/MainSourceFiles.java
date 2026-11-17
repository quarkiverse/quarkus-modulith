package io.quarkiverse.modulith.coverage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Finds the hand-written Java sources under {@code src/main} of every module, without descending into build output or
 * repository metadata.
 */
final class MainSourceFiles extends SimpleFileVisitor<Path> {

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(".git", "target", "node_modules");
    private static final String MAIN_SOURCES = "/src/main/";
    private static final String JAVA_SUFFIX = ".java";

    private final Path root;
    private final List<Path> files = new ArrayList<>();

    private MainSourceFiles(Path root) {
        this.root = root;
    }

    static List<Path> under(Path root) throws IOException {
        MainSourceFiles visitor = new MainSourceFiles(root);
        Files.walkFileTree(root, visitor);
        return List.copyOf(visitor.files);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
        return SKIPPED_DIRECTORIES.contains(String.valueOf(directory.getFileName()))
                ? FileVisitResult.SKIP_SUBTREE
                : FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        String relative = "/" + root.relativize(file).toString().replace('\\', '/');
        if (relative.endsWith(JAVA_SUFFIX) && relative.contains(MAIN_SOURCES)) {
            files.add(file);
        }
        return FileVisitResult.CONTINUE;
    }
}
