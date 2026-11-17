package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The class files of the given packages on a class path, read from directories and jars.
 */
final class ClassPathFiles implements ClassFiles {

    private static final String JAR_PROTOCOL = "jar";
    private static final String FILE_PROTOCOL = "file";
    private static final List<String> TEST_OUTPUTS = List.of("/test-classes/", "/classes/java/test/",
            "/classes/kotlin/test/", "/bin/test/", "/out/test/");

    private final ClassLoader classLoader;
    private final Predicate<URI> locations;
    private final Map<String, URI> found = new LinkedHashMap<>();

    /**
     * @param locations decides which class files to read, by their location
     */
    ClassPathFiles(ClassLoader classLoader, Predicate<URI> locations) {
        this.classLoader = classLoader;
        this.locations = locations;
    }

    /**
     * @return whether the location is outside the test output directories of Maven, Gradle and IDEs
     */
    static boolean isNotTestOutput(URI location) {
        String path = location.toString().replace('\\', '/');
        return TEST_OUTPUTS.stream().noneMatch(path::contains);
    }

    @Override
    public Set<String> names(List<String> packages) {
        packages.forEach(this::find);
        List<String> prefixes = packages.stream().map(it -> it + ".").toList();
        return found.keySet().stream().filter(name -> prefixes.stream().anyMatch(name::startsWith))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void find(String packageName) {
        String path = packageName.replace('.', '/');
        resources(classLoader, path).forEach(url -> classes(url, path)
                .filter(it -> locations.test(it.getValue()))
                .forEach(it -> found.putIfAbsent(it.getKey(), it.getValue())));
    }

    static List<URL> resources(ClassLoader classLoader, String path) {
        try {
            return Collections.list(classLoader.getResources(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @throws IllegalStateException for a location that is neither a directory nor a jar
     */
    static Stream<Map.Entry<String, URI>> classes(URL url, String path) {
        return switch (url.getProtocol()) {
            case JAR_PROTOCOL -> jarClasses(url, path);
            case FILE_PROTOCOL -> directoryClasses(url, path);
            default -> throw new IllegalStateException("Cannot read the classes of " + url
                    + ": only directories and jars are supported");
        };
    }

    private static Stream<Map.Entry<String, URI>> directoryClasses(URL url, String path) {
        try (Stream<Path> files = Files.walk(Path.of(url.toURI()))) {
            Path root = Path.of(url.toURI());
            return files.filter(it -> it.toString().endsWith(".class"))
                    .map(it -> Map.entry(className(path + "/" + root.relativize(it).toString().replace('\\', '/')),
                            it.toUri()))
                    .toList().stream();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot read the classes of " + url, e);
        }
    }

    private static Stream<Map.Entry<String, URI>> jarClasses(URL url, String path) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            connection.setUseCaches(false);
            return entries(connection.getJarFile(), url, path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<Map.Entry<String, URI>> entries(JarFile file, URL url, String path) throws IOException {
        try (JarFile jar = file) {
            String base = url.toString().substring(0, url.toString().indexOf("!/") + 2);
            return jar.stream().map(JarEntry::getName)
                    .filter(it -> it.startsWith(path + "/") && it.endsWith(".class"))
                    .map(it -> Map.entry(className(it), URI.create(base + it)))
                    .toList().stream();
        }
    }

    private static String className(String file) {
        return file.substring(0, file.length() - ".class".length()).replace('/', '.');
    }

    @Override
    public Optional<byte[]> read(String name) {
        return Optional.ofNullable(found.get(name)).map(ClassPathFiles::bytes);
    }

    /**
     * Reads without the {@code JarURLConnection} cache, which would keep the jar open until the JVM ends.
     */
    static byte[] bytes(URI location) {
        try {
            URLConnection connection = location.toURL().openConnection();
            connection.setUseCaches(false);
            return ModelAnalyzer.readAll(connection.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
