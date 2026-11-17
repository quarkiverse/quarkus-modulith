package io.quarkiverse.modulith.it.fixtures.broken;

import io.quarkiverse.modulith.Modulith;

/**
 * A test-only application for {@code TestApplicationModules.of}: invoices uses an internal type of orders. Its
 * {@code @Modulith} lies in the test sources, so the build of the application under test ignores it.
 */
@Modulith(systemName = "Broken")
public class BrokenApplication {
}
