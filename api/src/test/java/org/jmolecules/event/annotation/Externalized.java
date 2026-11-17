package org.jmolecules.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stands in for jMolecules' annotation, which the API reads by name. It has no {@code target} attribute, like older
 * jMolecules versions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Externalized {

    String value() default "";
}
