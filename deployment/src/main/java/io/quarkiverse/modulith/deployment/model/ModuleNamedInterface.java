package io.quarkiverse.modulith.deployment.model;

import java.util.Set;

/**
 * A named interface of a module: whole packages and single types exposed under a name.
 *
 * @param name the name
 * @param packages the annotated packages it was declared on
 * @param types the names of its types
 */
public record ModuleNamedInterface(String name, Set<String> packages, Set<String> types) {

    public boolean contains(String className) {
        return types.contains(className);
    }
}
