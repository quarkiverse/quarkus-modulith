package io.quarkiverse.modulith.deployment.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.impl.ClassFiles;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.paths.PathVisit;

/**
 * The class files of the application archives. Classes are read only when a package is asked for, and only from the
 * archives that contain classes of that package.
 */
public final class ArchiveClassFiles implements ClassFiles {

    private final Map<String, ApplicationArchive> owners;
    private final boolean includeTestClasses;
    private final Map<String, byte[]> bytes = new HashMap<>();
    private final Set<String> testClasses = new HashSet<>();

    /**
     * @param archives the application archives, the root archive first
     * @param includeTestClasses whether classes of test output directories are part of the application
     */
    public ArchiveClassFiles(List<ApplicationArchive> archives, boolean includeTestClasses) {
        this.owners = new LinkedHashMap<>();
        archives.forEach(archive -> archive.getIndex().getKnownClasses()
                .forEach(type -> owners.putIfAbsent(type.name().toString(), archive)));
        this.includeTestClasses = includeTestClasses;
    }

    /**
     * @return the names of all classes of the archives
     */
    public Set<String> classNames() {
        return Collections.unmodifiableSet(owners.keySet());
    }

    @Override
    public Set<String> names(List<String> packages) {
        List<String> prefixes = packages.stream().map(it -> it.isEmpty() ? "" : it + ".").toList();
        Map<ApplicationArchive, List<String>> byArchive = owners.entrySet().stream()
                .filter(it -> prefixes.stream().anyMatch(it.getKey()::startsWith))
                .filter(it -> !bytes.containsKey(it.getKey()))
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        byArchive.forEach(this::readAll);
        return bytes.keySet().stream()
                .filter(name -> prefixes.stream().anyMatch(name::startsWith))
                .filter(name -> includeTestClasses || !testClasses.contains(name))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void readAll(ApplicationArchive archive, List<String> names) {
        archive.accept(tree -> names.forEach(name -> tree.accept(name.replace('.', '/') + ".class",
                visit -> read(visit, name))));
    }

    void read(PathVisit visit, String name) {
        if (Objects.isNull(visit)) {
            return;
        }
        if (isTestOutput(visit.getRoot())) {
            testClasses.add(name);
        }
        bytes.put(name, readBytes(visit.getPath()));
    }

    @Override
    public Optional<byte[]> read(String name) {
        if (!bytes.containsKey(name)) {
            Optional.ofNullable(owners.get(name)).ifPresent(it -> readAll(it, List.of(name)));
        }
        return Optional.ofNullable(bytes.get(name));
    }

    /**
     * Reads the class file if it was not read yet.
     *
     * @return whether the class comes from a test output directory
     */
    public boolean isTestClass(String name) {
        read(name);
        return testClasses.contains(name);
    }

    static byte[] readBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Recognizes the test output directories of Maven and Gradle builds.
     */
    static boolean isTestOutput(Path root) {
        if (Objects.isNull(root)) {
            return false;
        }
        String path = root.toString().replace('\\', '/');
        return path.endsWith("/test-classes") || path.contains("/classes/java/test")
                || path.contains("/classes/kotlin/test");
    }
}
