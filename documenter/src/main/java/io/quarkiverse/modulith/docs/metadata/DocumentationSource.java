package io.quarkiverse.modulith.docs.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Javadoc of the application, read from the {@code META-INF/quarkus-modulith/javadoc.json} files that
 * {@code quarkus-modulith-apt} writes. The counterpart of Spring Modulith's {@code SpringModulithDocumentationSource}.
 */
public final class DocumentationSource {

    /**
     * Where the annotation processor writes the Javadoc, relative to the class output directory.
     */
    public static final String LOCATION = "META-INF/quarkus-modulith/javadoc.json";

    private static final DocumentationSource NONE = new DocumentationSource(List.of());

    private final Map<String, TypeMetadata> metadata;

    /**
     * @param metadata the metadata; of several entries for one name the first wins
     */
    private DocumentationSource(List<TypeMetadata> metadata) {
        Map<String, TypeMetadata> byName = new HashMap<>();
        metadata.forEach(it -> byName.putIfAbsent(it.name(), it));
        this.metadata = Collections.unmodifiableMap(byName);
    }

    /**
     * @return a source without any documentation
     */
    public static DocumentationSource none() {
        return NONE;
    }

    /**
     * @return a source over the given metadata
     */
    public static DocumentationSource of(List<TypeMetadata> metadata) {
        return new DocumentationSource(metadata);
    }

    /**
     * @param json the content of a {@code javadoc.json} file
     * @return the metadata it contains
     * @throws IllegalArgumentException when the content is not the expected JSON
     */
    public static List<TypeMetadata> parse(String json) {
        return JavadocJson.read(json);
    }

    /**
     * @return a source over every {@code javadoc.json} on the class path of the thread context class loader, or of the
     *         class loader of this class when the thread has none
     */
    public static DocumentationSource fromClassPath() {
        return fromClassLoader(Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(DocumentationSource.class::getClassLoader));
    }

    /**
     * @throws IllegalArgumentException when a {@code javadoc.json} is not the expected JSON, naming its URL
     */
    static DocumentationSource fromClassLoader(ClassLoader classLoader) {
        try {
            List<TypeMetadata> metadata = new ArrayList<>();
            for (URL url : Collections.list(classLoader.getResources(LOCATION))) {
                metadata.addAll(read(url));
            }
            return of(metadata);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + LOCATION, e);
        }
    }

    private static List<TypeMetadata> read(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(url + ": " + e.getMessage(), e);
        }
    }

    /**
     * @return the Javadoc of the type or package with the given name, if it has any text
     */
    public Optional<String> getDocumentation(String typeOrPackage) {
        return find(typeOrPackage).flatMap(TypeMetadata::comment).filter(it -> !it.isBlank());
    }

    /**
     * @param type the binary name of the declaring type
     * @param signature the method signature, see {@link MethodMetadata#signature()}
     * @return the Javadoc of the method, if it has any text
     */
    public Optional<String> getDocumentation(String type, String signature) {
        return find(type).stream()
                .flatMap(it -> it.methods().stream())
                .filter(it -> it.signature().equals(signature))
                .findFirst()
                .flatMap(MethodMetadata::comment)
                .filter(it -> !it.isBlank());
    }

    private Optional<TypeMetadata> find(String name) {
        return Optional.ofNullable(metadata.get(name));
    }
}
