package io.quarkiverse.modulith.deployment.model;

import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.JavaPackage;

/**
 * Only the {@code second} package is a module.
 */
public class SecondOnlyStrategy implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
        return rootPackage.getDirectSubPackages().stream().filter(it -> it.getLocalName().equals("second"));
    }
}
