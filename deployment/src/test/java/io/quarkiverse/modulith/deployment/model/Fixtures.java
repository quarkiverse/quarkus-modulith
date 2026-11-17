package io.quarkiverse.modulith.deployment.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

/**
 * Builds module models from the compiled test fixtures, without starting Quarkus.
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static ModuleModel model(String basePackage, Optional<String> configured) {
        return model(classes(basePackage), configured, Set.of());
    }

    /**
     * @return a builder for the given classes, indexed with Jandex
     */
    public static ModuleModelBuilder builder(Map<String, byte[]> classes) {
        return new ModuleModelBuilder(index(classes), classes.keySet(), classes::get);
    }

    /**
     * @param extraClasses names added to the application classes although they have neither index entry nor bytes
     */
    public static ModuleModel model(Map<String, byte[]> classes, Optional<String> configured, Set<String> extraClasses) {
        Set<String> names = new HashSet<>(classes.keySet());
        names.addAll(extraClasses);
        return new ModuleModelBuilder(index(classes), names, classes::get).basePackage(configured).build();
    }

    public static Index index(Map<String, byte[]> classes) {
        Indexer indexer = new Indexer();
        classes.values().forEach(bytes -> {
            try {
                indexer.index(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return indexer.complete();
    }

    public static Map<String, byte[]> classes(String basePackage) {
        try {
            Path root = Path.of(Fixtures.class.getResource("/" + basePackage.replace('.', '/')).toURI());
            Path classesRoot = root;
            for (int i = 0; i < basePackage.split("\\.").length; i++) {
                classesRoot = classesRoot.getParent();
            }
            Map<String, byte[]> result = new HashMap<>();
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                    String relative = classesRoot.relativize(file).toString();
                    String name = relative.substring(0, relative.length() - ".class".length()).replace('/', '.')
                            .replace('\\', '.');
                    try (InputStream in = Files.newInputStream(file)) {
                        result.put(name, in.readAllBytes());
                    }
                }
            }
            return result;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
