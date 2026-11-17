package io.quarkiverse.modulith.docs;

import io.quarkiverse.modulith.core.DependencyType;

/**
 * A reference from a type of one module to a type of another module.
 *
 * @param sourceType the binary name of the referencing type
 * @param targetType the binary name of the referenced type
 * @param type the kind of reference
 * @param targetModule the identifier of the module of the referenced type
 */
public record ModuleDependency(String sourceType, String targetType, DependencyType type, String targetModule) {
}
