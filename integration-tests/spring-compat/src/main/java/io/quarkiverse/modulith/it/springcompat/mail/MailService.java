package io.quarkiverse.modulith.it.springcompat.mail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.auth.AuthService;
import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the mail module.
 */
@ApplicationScoped
public class MailService {

    @Inject
    Clock clock;

    @Inject
    AuthService auth;

    public String name() {
        return "mail";
    }
}
