package io.smallrye.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Test stand-in for the SmallRye Config annotation. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigMapping {
    String prefix() default "";
}
