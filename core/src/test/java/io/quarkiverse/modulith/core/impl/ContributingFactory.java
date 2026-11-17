package io.quarkiverse.modulith.core.impl;

import java.util.List;

import io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy;
import io.quarkiverse.modulith.core.ApplicationModuleSourceFactory;

/**
 * Contributes the {@code fixtures.contrib} root, detected with the explicitly-annotated strategy, and the listed module
 * {@code fixtures.listed.alpha}.
 */
public class ContributingFactory implements ApplicationModuleSourceFactory {

    @Override
    public List<String> getRootPackages() {
        return List.of("io.quarkiverse.modulith.core.fixtures.contrib");
    }

    @Override
    public List<String> getModuleBasePackages() {
        return List.of("io.quarkiverse.modulith.core.fixtures.listed.alpha");
    }

    @Override
    public ApplicationModuleDetectionStrategy getApplicationModuleDetectionStrategy() {
        return ApplicationModuleDetectionStrategy.explicitlyAnnotated();
    }
}
