package io.quarkiverse.modulith.docs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Where the {@link Documenter} writes and whether it empties the output first, Spring Modulith's
 * {@code Documenter.Options}.
 */
public final class DocumenterOptions {

    /**
     * The default output folder: {@code target/modulith-docs} in a Maven build, {@code build/modulith-docs} otherwise.
     */
    public static final String DEFAULT_LOCATION = location(Path.of(""));

    private final DocumentationOutput output;
    private final boolean clean;

    private DocumenterOptions(DocumentationOutput output, boolean clean) {
        this.output = output;
        this.clean = clean;
    }

    /**
     * @return {@code target/modulith-docs} when the folder holds a {@code pom.xml}, {@code build/modulith-docs}
     *         otherwise
     */
    static String location(Path projectFolder) {
        return (Files.exists(projectFolder.resolve("pom.xml")) ? "target" : "build") + "/modulith-docs";
    }

    /**
     * @return writes to {@link #DEFAULT_LOCATION} and empties it before the first file is written
     */
    public static DocumenterOptions defaults() {
        return new DocumenterOptions(new FolderOutput(Path.of(DEFAULT_LOCATION)), true);
    }

    /**
     * @return options keeping existing files in the output
     */
    public DocumenterOptions withoutClean() {
        return new DocumenterOptions(output, false);
    }

    /**
     * With {@code clean}, which is on by default, the folder is emptied before the first file is written. To protect the
     * sources, emptying fails for a file system root, the working directory or one of its ancestors, a folder holding a
     * {@code pom.xml}, {@code build.gradle} or {@code build.gradle.kts}, and, inside the working directory, anything but
     * a sub-folder of {@code target} or {@code build}.
     *
     * @param folder the output folder, {@code null} for {@link #DEFAULT_LOCATION}
     * @throws IllegalArgumentException when the folder is empty or blank, which would name the working directory
     */
    public DocumenterOptions withOutputFolder(String folder) {
        if (Objects.nonNull(folder) && folder.isBlank()) {
            throw new IllegalArgumentException("Output folder must not be empty!");
        }
        return new DocumenterOptions(new FolderOutput(Path.of(Objects.isNull(folder) ? DEFAULT_LOCATION : folder)),
                clean);
    }

    /**
     * The given output receives the diagrams, canvases and the aggregating document. The module metadata of
     * {@link Documenter#writeModuleMetadata()}, which {@link Documenter#writeDocumentation()} writes too, still goes to
     * the resource output folder of the build on disk, since it is packaged with the application.
     *
     * @return options writing to the given output instead of a folder
     */
    public DocumenterOptions withOutput(DocumentationOutput output) {
        return new DocumenterOptions(Objects.requireNonNull(output, "output"), clean);
    }

    DocumentationOutput output() {
        return output;
    }

    boolean clean() {
        return clean;
    }
}
