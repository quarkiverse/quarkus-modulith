package io.quarkiverse.modulith.it.springcompat.reporting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.springcompat.common.time.Clock;

/**
 * The API of the reporting module.
 */
@ApplicationScoped
public class ReportingService {

    @Inject
    Clock clock;

    public String name() {
        return "reporting";
    }
}
