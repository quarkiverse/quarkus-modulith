package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a sub-package (when placed on its {@code package-info.java}) or a single type of a module to other modules
 * under a name. Other modules can then refer to it in {@link ApplicationModule#allowedDependencies()} as
 * {@code module::name}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface NamedInterface {

    /**
     * The names of the interface. Defaults to the last segment of the package name, for a package as for a type,
     * which is why a type of a module's base package must name its interface.
     */
    String[] value() default {};

    /**
     * Alias of {@link #value()}. The names of both attributes are merged.
     */
    String[] name() default {};

    /**
     * Whether types that appear in the public method signatures of an annotated type, and live in the same module,
     * join the named interface too, transitively. Like in Spring Modulith 2.0 the default is {@code true}.
     */
    boolean propagate() default true;
}
