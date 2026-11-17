package io.quarkiverse.modulith.deployment.docs;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.LaunchMode;

/**
 * Writes the documentation when it is enabled and the application is packaged, or in dev mode when
 * {@code quarkus.modulith.docs.dev-mode} is set.
 */
public final class DocsActive implements BooleanSupplier {

    private final DocsConfig config;
    private final LaunchMode launchMode;

    DocsActive(DocsConfig config, LaunchMode launchMode) {
        this.config = config;
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled() && (launchMode == LaunchMode.NORMAL
                || launchMode == LaunchMode.DEVELOPMENT && config.devMode());
    }
}
