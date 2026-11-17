package io.quarkiverse.modulith.core.impl;

import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.JavaPackage;

/**
 * A second registered strategy, which makes the registration ambiguous.
 */
public class OtherStrategy implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
        return Stream.empty();
    }
}
