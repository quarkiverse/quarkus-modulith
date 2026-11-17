package io.quarkiverse.modulith.deployment.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.paths.PathVisit;

/**
 * Reads resources from the application archives. Class files come from the module model, see
 * {@link io.quarkiverse.modulith.deployment.model.ArchiveClassFiles}.
 */
public final class ArchiveResources {

    private final List<ApplicationArchive> archives;

    public ArchiveResources(List<ApplicationArchive> archives) {
        this.archives = List.copyOf(archives);
    }

    /**
     * @return the content of the resource in every archive that has it, in archive order
     */
    public List<byte[]> all(String resource) {
        List<byte[]> result = new ArrayList<>();
        for (ApplicationArchive archive : archives) {
            byte[] bytes = archive.apply(tree -> tree.apply(resource, ArchiveResources::read));
            if (Objects.nonNull(bytes)) {
                result.add(bytes);
            }
        }
        return result;
    }

    static byte[] read(PathVisit visit) {
        return Objects.isNull(visit) ? null : read(visit.getPath());
    }

    static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }
}
