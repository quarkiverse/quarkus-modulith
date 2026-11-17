package io.quarkiverse.modulith.core.fixtures.shop;

import io.quarkiverse.modulith.Modulith;

@Modulith(systemName = "Shop", sharedModules = "kernel", additionalPackages = "io.quarkiverse.modulith.core.fixtures.shop.order.nested")
public class ShopApplication {
}
