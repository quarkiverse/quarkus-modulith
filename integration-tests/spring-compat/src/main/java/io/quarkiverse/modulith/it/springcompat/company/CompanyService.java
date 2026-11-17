package io.quarkiverse.modulith.it.springcompat.company;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the company module.
 */
@ApplicationScoped
public class CompanyService {

    @Inject
    Clock clock;

    public String name() {
        return "company";
    }
}
