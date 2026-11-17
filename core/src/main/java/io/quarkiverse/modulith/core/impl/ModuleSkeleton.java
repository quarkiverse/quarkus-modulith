package io.quarkiverse.modulith.core.impl;

import io.quarkiverse.modulith.core.ApplicationModuleSource;
import io.quarkiverse.modulith.core.JavaPackage;

/**
 * A detected module before its analysis.
 *
 * @param source where the module comes from
 * @param basePackage the module's base package without nested modules
 */
record ModuleSkeleton(ApplicationModuleSource source, JavaPackage basePackage) {
}
