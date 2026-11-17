package io.quarkiverse.modulith.it.springcompat.fiscal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the fiscal module.
 */
@ApplicationScoped
public class FiscalService {

    @Inject
    Clock clock;

    public String name() {
        return "fiscal";
    }
}
