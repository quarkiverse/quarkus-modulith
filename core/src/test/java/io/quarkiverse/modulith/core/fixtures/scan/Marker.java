package io.quarkiverse.modulith.core.fixtures.scan;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Marker {

    Class<?>[] types() default {};

    Thread.State[] states() default {};

    Deprecated nested() default @Deprecated;

    Deprecated[] many() default {};

    int[] numbers() default {};
}
