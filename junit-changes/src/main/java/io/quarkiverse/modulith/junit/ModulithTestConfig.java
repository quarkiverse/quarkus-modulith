package io.quarkiverse.modulith.junit;

import java.util.Objects;

import org.eclipse.microprofile.config.Config;

import io.smallrye.config.PropertiesConfigSourceLoader;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Reads the configuration of change-aware test execution with SmallRye Config, without starting Quarkus: system
 * properties, environment variables, {@code application.properties} and
 * {@code META-INF/microprofile-config.properties} on the class path, with the {@code test} profile active.
 */
final class ModulithTestConfig {

    /**
     * The ordinal Quarkus gives {@code application.properties} on the class path.
     */
    static final int APPLICATION_PROPERTIES_ORDINAL = 250;

    private ModulithTestConfig() {
    }

    /**
     * @return the configuration as seen from the context class loader of the current thread
     */
    static Config load() {
        return load(Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(),
                ModulithTestConfig.class.getClassLoader()));
    }

    /**
     * @param classLoader the class loader to find the property files with
     * @return the configuration
     */
    static Config load(ClassLoader classLoader) {
        return new SmallRyeConfigBuilder()
                .forClassLoader(classLoader)
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(PropertiesConfigSourceLoader.inClassPath("application.properties",
                        APPLICATION_PROPERTIES_ORDINAL, classLoader))
                .withProfile("test")
                .build();
    }
}
