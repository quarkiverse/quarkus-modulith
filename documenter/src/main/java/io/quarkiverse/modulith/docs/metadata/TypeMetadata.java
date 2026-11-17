package io.quarkiverse.modulith.docs.metadata;

import java.util.List;
import java.util.Optional;

/**
 * The Javadoc of a type or package, as recorded by {@code quarkus-modulith-apt}.
 *
 * @param name the binary name of the type, or the package name
 * @param comment the first part of the Javadoc, before the block tags
 * @param methods the documented methods
 */
public record TypeMetadata(String name, Optional<String> comment, List<MethodMetadata> methods) {

    public TypeMetadata {
        methods = List.copyOf(methods);
    }
}
