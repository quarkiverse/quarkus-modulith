package io.quarkiverse.modulith.deployment.docs.fixtures.app.inventory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;

@Stereotype
@ApplicationScoped
@Retention(RetentionPolicy.RUNTIME)
public @interface Gateway {
}
