package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the base package of a modular application. Every direct sub-package of the package of the annotated type is an
 * application module.
 * <p>
 * Unlike Spring Modulith's {@code @Modulith} it is not a {@code @SpringBootApplication}: Quarkus needs no main
 * class. {@link Modulithic} is an equivalent alias.
 * <p>
 * The annotation is optional. Without it the base package comes from {@code quarkus.modulith.base-package}, or else is
 * the longest package prefix shared by all application classes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Modulith {

    /**
     * The name of the system, used as the title of the generated documentation. Defaults to the simple name of the
     * annotated type.
     */
    String systemName() default "";

    /**
     * Identifiers of modules every other module may depend on, in addition to its declared allowed dependencies.
     */
    String[] sharedModules() default {};

    /**
     * Whether module identifiers are the full package names of the modules instead of their name relative to the
     * base package.
     */
    boolean useFullyQualifiedModuleNames() default false;

    /**
     * Further root packages whose direct sub-packages are modules too. Packages nested in the base package are
     * ignored.
     */
    String[] additionalPackages() default {};
}
