package io.quarkiverse.modulith.core;

/**
 * A place in the code, rendered relative to a module. Like Spring Modulith's {@code Source}.
 */
@FunctionalInterface
public interface Source {

    /**
     * @param module the module to abbreviate type names relative to
     * @return the rendered place, for example {@code c.a.o.OrderManagement.complete(…)}
     */
    String toString(ApplicationModule module);
}
