package io.quarkiverse.modulith.it;

import io.quarkiverse.modulith.Modulith;

/**
 * Marks the base package of the shop. Its direct sub-packages are the modules.
 */
@Modulith(systemName = "Shop", sharedModules = "common")
public class ShopApplication {
}
