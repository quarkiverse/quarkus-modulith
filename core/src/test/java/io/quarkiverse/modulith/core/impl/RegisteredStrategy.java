package io.quarkiverse.modulith.core.impl;

import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.JavaPackage;

/**
 * A strategy registered as service: only the sub-package {@code order} is a module.
 */
public class RegisteredStrategy implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
        return rootPackage.getDirectSubPackages().stream().filter(it -> it.getLocalName().equals("order"));
    }
}
