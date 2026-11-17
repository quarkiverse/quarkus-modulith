package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Customizes an application module. Put it on the {@code package-info.java} of a module's base package, or on a type
 * of that package annotated with {@link PackageInfo}. On a sub-package of a module it declares a nested module.
 * <p>
 * The attribute names and semantics follow Spring Modulith's {@code @ApplicationModule} so that existing module
 * declarations can be moved over by changing the import.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface ApplicationModule {

    /**
     * The value Spring Modulith uses as default of {@code allowedDependencies} to tell an absent attribute from an
     * empty one. Quarkus Modulith reads the class file and does not need it. Kept for source compatibility.
     */
    String OPEN_TOKEN = "¯\\_(ツ)_/¯";

    /**
     * The identifier of the module. Defaults to the name of the module's base package relative to the root package,
     * for example {@code order} or, for a nested module, {@code order.pricing}. Must not contain {@code ::}.
     */
    String id() default "";

    /**
     * A human readable name used in documentation and in the Dev UI. Defaults to the capitalized last segment of the
     * base package.
     */
    String displayName() default "";

    /**
     * The modules this module may depend on.
     * <p>
     * Leaving the attribute out does not restrict dependencies at all. Declaring an empty array allows no dependencies
     * to other modules. Entries are module identifiers ({@code inventory}, which grants the module's base package),
     * {@code inventory::events} for a single named interface, {@code inventory::*} for all named interfaces (without
     * the base package), and {@code inventory::**} for everything the module exposes.
     */
    String[] allowedDependencies() default {};

    /**
     * The type of the module.
     */
    Type type() default Type.CLOSED;

    enum Type {

        /**
         * Only the base package and named interfaces may be referenced by other modules. A closed module must not be
         * part of a dependency cycle.
         */
        CLOSED,

        /**
         * Every type of the module may be referenced by other modules, and the module is left out of cycle detection.
         * Useful for a shared kernel such as a {@code common} module.
         */
        OPEN
    }
}
