package io.quarkiverse.modulith.runtime.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationModulesRuntime;
import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.core.data.ModelCodec;
import io.quarkus.arc.Unremovable;

/**
 * Produces the {@link ApplicationModules} bean from the model the build wrote to {@link ModelCodec#LOCATION}, when the
 * bean is first used, and combines it with the application runtime into the {@link ApplicationModulesRuntime} bean.
 * Reading the model from a resource keeps the startup code the same size whatever the size of the application.
 */
@Singleton
public class ApplicationModulesRuntimeProducer {

    // Unremovable: quarkus-modulith-junit looks it up with Arc.container().instance(ApplicationModules.class)
    @Produces
    @Singleton
    @Unremovable
    ApplicationModules applicationModules() {
        return new ApplicationModules(ModelCodec.decode(read(Thread.currentThread().getContextClassLoader())));
    }

    @Produces
    @Singleton
    ApplicationModulesRuntime applicationModulesRuntime(ApplicationModules modules, ApplicationRuntime runtime) {
        return new ApplicationModulesRuntime(modules, runtime);
    }

    static byte[] read(ClassLoader loader) {
        try (InputStream in = loader.getResourceAsStream(ModelCodec.LOCATION)) {
            if (Objects.isNull(in)) {
                throw new IllegalStateException(ModelCodec.LOCATION
                        + " is missing, the application was not built with the quarkus-modulith extension");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + ModelCodec.LOCATION, e);
        }
    }
}
