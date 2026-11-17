package io.quarkiverse.modulith.core.par.strategy;

import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.JavaPackage;

/**
 * Modules are the sub-packages of {@code modules}, for Quarkus Modulith.
 */
public class QuarkusModulesStrategy implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
        return rootPackage.getSubPackagesMatching((pkg, trailing) -> trailing.matches("modules\\.[^.]+"));
    }
}
