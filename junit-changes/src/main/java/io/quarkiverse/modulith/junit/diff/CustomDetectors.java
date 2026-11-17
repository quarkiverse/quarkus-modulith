package io.quarkiverse.modulith.junit.diff;

import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Instantiates a {@link FileModificationDetector} selected by its fully qualified class name.
 */
final class CustomDetectors {

    private static final System.Logger LOG = System.getLogger(CustomDetectors.class.getName());

    private CustomDetectors() {
    }

    /**
     * @param className the fully qualified name of a {@link FileModificationDetector} with a no-argument constructor,
     *        which may be non-public. It is loaded by the thread context class loader of the test, or by the class
     *        loader of this SPI when there is none.
     * @throws IllegalStateException if the class cannot be loaded or instantiated
     * @throws IllegalArgumentException if the class does not implement {@link FileModificationDetector}
     */
    static FileModificationDetector instantiate(String className) {
        var type = load(className);
        if (!FileModificationDetector.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(
                    "%s does not implement %s!".formatted(className, FileModificationDetector.class.getName()));
        }
        LOG.log(Level.INFO, "Found request via property for file modification detector ''{0}''", className);
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (FileModificationDetector) constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            throw failure(className, e);
        }
    }

    private static Class<?> load(String className) {
        var loader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(FileModificationDetector.class::getClassLoader);
        try {
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw failure(className, e);
        }
    }

    private static IllegalStateException failure(String className, Throwable cause) {
        return new IllegalStateException("Unable to instantiate file modification detector %s".formatted(className),
                cause);
    }
}
