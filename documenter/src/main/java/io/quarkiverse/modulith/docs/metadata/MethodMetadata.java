package io.quarkiverse.modulith.docs.metadata;

import java.util.Optional;

/**
 * The Javadoc of a method, as recorded by {@code quarkus-modulith-apt}.
 *
 * @param name the method name
 * @param signature the name and parameter types, for example {@code complete(com.acme.Order, int)}
 * @param comment the first part of the Javadoc, before the block tags
 */
public record MethodMetadata(String name, String signature, Optional<String> comment) {
}
