package io.quarkiverse.modulith.it.springcompat.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the auth module.
 */
@ApplicationScoped
public class AuthService {

    @Inject
    Clock clock;

    public String name() {
        return "auth";
    }
}
