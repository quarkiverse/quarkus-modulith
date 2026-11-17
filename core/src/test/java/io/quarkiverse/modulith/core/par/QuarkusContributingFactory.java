package io.quarkiverse.modulith.core.par;

import java.util.List;

import io.quarkiverse.modulith.core.ApplicationModuleSourceFactory;

/**
 * Contributes the {@code parcontrib} root package and the listed module {@code parcontrib.listed}, for Quarkus Modulith.
 */
public class QuarkusContributingFactory implements ApplicationModuleSourceFactory {

    @Override
    public List<String> getRootPackages() {
        return List.of("io.quarkiverse.modulith.core.parcontrib");
    }

    @Override
    public List<String> getModuleBasePackages() {
        return List.of("io.quarkiverse.modulith.core.parcontrib.listed");
    }
}
