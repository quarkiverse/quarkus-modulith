package io.quarkus.scheduler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Test stand-in for the Quarkus scheduler annotation. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    String every() default "";
}
