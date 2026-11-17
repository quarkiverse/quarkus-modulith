package io.quarkiverse.modulith.junit;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.Modulith;
import io.quarkiverse.modulith.Modulithic;

/**
 * Finds the base package of the application a test belongs to: the closest package, starting at the package of the
 * test and walking up, that declares exactly one type annotated with {@link Modulith} or {@link Modulithic}. Like
 * Spring Boot's lookup of the {@code @SpringBootConfiguration} class, several annotated types in that package make the
 * lookup fail. Annotations are matched by name, so types loaded by another class loader than this class are found too.
 */
final class BasePackageFinder {

    private static final System.Logger LOG = System.getLogger(BasePackageFinder.class.getName());

    private static final Set<String> APPLICATION_ANNOTATIONS = Set.of(Modulith.class.getName(),
            Modulithic.class.getName());

    private static final String CLASS_SUFFIX = ".class";

    private final Map<Map.Entry<ClassLoader, String>, Optional<String>> cache = new ConcurrentHashMap<>();

    /**
     * @param packageName the package of the test class
     * @param classLoader the class loader of the test class, must not be {@code null}
     * @return the application base package, empty if none or more than one candidate was found, cached per class
     *         loader and package
     */
    Optional<String> find(String packageName, ClassLoader classLoader) {
        return cache.computeIfAbsent(Map.entry(classLoader, packageName), key -> lookup(packageName, classLoader));
    }

    private static Optional<String> lookup(String packageName, ClassLoader classLoader) {
        for (var current = packageName; !current.isEmpty(); current = parentOf(current)) {
            var candidates = applicationTypesIn(current, classLoader);
            if (candidates.size() == 1) {
                return Optional.of(current);
            }
            if (candidates.size() > 1) {
                LOG.log(Level.WARNING, "Found multiple application types in package {0}: {1}", current, candidates);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static String parentOf(String packageName) {
        var lastDot = packageName.lastIndexOf('.');
        return lastDot < 0 ? "" : packageName.substring(0, lastDot);
    }

    private static Set<String> applicationTypesIn(String packageName, ClassLoader classLoader) {
        var path = packageName.replace('.', '/');
        return resources(path, classLoader).stream()
                .flatMap(url -> classNames(url, path, packageName))
                .distinct()
                .filter(className -> isApplicationType(className, classLoader))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static List<URL> resources(String path, ClassLoader classLoader) {
        try {
            return Collections.list(classLoader.getResources(path));
        } catch (IOException e) {
            LOG.log(Level.DEBUG, "Unable to list class path resources " + path, e);
            return List.of();
        }
    }

    private static Stream<String> classNames(URL url, String path, String packageName) {
        try {
            return switch (url.getProtocol()) {
                case "file" -> directoryClassNames(Path.of(url.toURI()), packageName);
                case "jar" -> jarClassNames((JarURLConnection) url.openConnection(), path);
                default -> Stream.empty();
            };
        } catch (IOException | URISyntaxException e) {
            LOG.log(Level.DEBUG, "Unable to list the classes in " + url, e);
            return Stream.empty();
        }
    }

    private static Stream<String> directoryClassNames(Path directory, String packageName) throws IOException {
        try (var files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(CLASS_SUFFIX))
                    .map(name -> packageName + "." + name.substring(0, name.length() - CLASS_SUFFIX.length()))
                    .toList()
                    .stream();
        }
    }

    private static Stream<String> jarClassNames(JarURLConnection connection, String path) throws IOException {
        connection.setUseCaches(false);
        try (var jar = connection.getJarFile()) {
            return jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> isClassDirectlyIn(name, path))
                    .map(name -> name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.'))
                    .toList()
                    .stream();
        }
    }

    private static boolean isClassDirectlyIn(String entryName, String path) {
        return entryName.startsWith(path + "/")
                && entryName.endsWith(CLASS_SUFFIX)
                && entryName.indexOf('/', path.length() + 1) < 0;
    }

    private static boolean isApplicationType(String className, ClassLoader classLoader) {
        try {
            return Arrays.stream(Class.forName(className, false, classLoader).getAnnotations())
                    .map(Annotation::annotationType)
                    .map(Class::getName)
                    .anyMatch(APPLICATION_ANNOTATIONS::contains);
        } catch (ClassNotFoundException | LinkageError e) {
            LOG.log(Level.DEBUG, "Unable to load " + className, e);
            return false;
        }
    }
}
