package io.quarkiverse.modulith.test.slice.billing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;

@Stereotype
@ApplicationScoped
@Retention(RetentionPolicy.RUNTIME)
public @interface SliceStereotype {
}
