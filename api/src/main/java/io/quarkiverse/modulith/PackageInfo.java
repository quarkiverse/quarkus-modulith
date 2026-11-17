package io.quarkiverse.modulith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a stand-in for {@code package-info.java}: the other annotations of the type, such as
 * {@link ApplicationModule} or {@link NamedInterface}, apply to its package. Useful in languages without package
 * annotations, like Kotlin.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PackageInfo {
}
