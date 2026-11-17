package io.quarkiverse.modulith.core.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Class loaders over the test class path plus a directory with extra resources, such as service registrations.
 */
final class ServiceClassLoaders {

    private ServiceClassLoaders() {
    }

    /**
     * @param directory where to write the resources
     * @param resources resource names and contents
     * @return the class loader, to close once the test is done with it
     */
    static URLClassLoader with(Path directory, Map<String, String> resources) {
        try {
            for (Map.Entry<String, String> resource : resources.entrySet()) {
                Path file = directory.resolve(resource.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, resource.getValue());
            }
            return new URLClassLoader(new URL[] { directory.toUri().toURL() }, ServiceClassLoaders.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the result of the action, run with the given class loader as context class loader
     */
    static <T> T callWith(ClassLoader classLoader, Supplier<T> action) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            return action.get();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    static String strategies() {
        return "META-INF/services/io.quarkiverse.modulith.core.ApplicationModuleDetectionStrategy";
    }

    static String factories() {
        return "META-INF/services/io.quarkiverse.modulith.core.ApplicationModuleSourceFactory";
    }
}
