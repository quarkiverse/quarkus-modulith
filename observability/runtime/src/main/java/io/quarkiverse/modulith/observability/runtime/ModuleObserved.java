package io.quarkiverse.modulith.observability.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Marks the beans whose calls from other modules are observed. Added by the build to the module beans Spring Modulith
 * would observe; not meant for application code.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ModuleObserved {
}
