package io.quarkiverse.modulith.observability.runtime;

import java.util.Objects;

/**
 * The module whose code runs on the current thread, so that calls inside a module are not observed again.
 */
final class CurrentModule {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CurrentModule() {
    }

    static String get() {
        return CURRENT.get();
    }

    /**
     * @return the previous module, to pass to {@link #restore(String)}
     */
    static String enter(String module) {
        String previous = CURRENT.get();
        CURRENT.set(module);
        return previous;
    }

    static void restore(String previous) {
        if (Objects.isNull(previous)) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
