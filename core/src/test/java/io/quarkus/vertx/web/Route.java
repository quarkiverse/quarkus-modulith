package io.quarkus.vertx.web;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Test stand-in for the Quarkus route annotation. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
    String path() default "";

    String[] methods() default {};
}
