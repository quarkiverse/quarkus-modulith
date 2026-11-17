package io.quarkiverse.modulith.core;

import java.util.List;

/**
 * The qualified dependencies of a module resolved against the model they belong to.
 *
 * @param modules the model the target modules were looked up in
 * @param dependencies the resolved dependencies
 */
record ResolvedDependencies(ApplicationModules modules, List<DefaultApplicationModuleDependency> dependencies) {
}
