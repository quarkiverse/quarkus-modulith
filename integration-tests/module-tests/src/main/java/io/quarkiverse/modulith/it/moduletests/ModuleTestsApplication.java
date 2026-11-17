package io.quarkiverse.modulith.it.moduletests;

import io.quarkiverse.modulith.Modulith;

/**
 * Modules: catalog, order (uses catalog), inventory (listens to order), shipping (uses inventory), audit
 * (independent) and the shared module shared.
 */
@Modulith(systemName = "Module tests", sharedModules = "shared")
public class ModuleTestsApplication {
}
