package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Same as {@link Modulith}. Spring Modulith offers it for applications that cannot use the
 * {@code @SpringBootApplication} meta-annotation. Quarkus Modulith treats both annotations alike.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Modulithic {

    /**
     * @see Modulith#systemName()
     */
    String systemName() default "";

    /**
     * @see Modulith#sharedModules()
     */
    String[] sharedModules() default {};

    /**
     * @see Modulith#useFullyQualifiedModuleNames()
     */
    boolean useFullyQualifiedModuleNames() default false;

    /**
     * @see Modulith#additionalPackages()
     */
    String[] additionalPackages() default {};
}
