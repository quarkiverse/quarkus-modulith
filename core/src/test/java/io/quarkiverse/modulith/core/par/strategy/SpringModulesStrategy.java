package io.quarkiverse.modulith.core.par.strategy;

import java.util.stream.Stream;

import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.JavaPackage;

/**
 * Modules are the sub-packages of {@code modules}, for Spring Modulith.
 */
public class SpringModulesStrategy implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
        return rootPackage.getSubPackagesMatching((pkg, trailing) -> trailing.matches("modules\\.[^.]+"));
    }
}
