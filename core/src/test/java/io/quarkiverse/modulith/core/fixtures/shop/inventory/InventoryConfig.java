package io.quarkiverse.modulith.core.fixtures.shop.inventory;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "inventory")
public interface InventoryConfig {
    int threshold();
}
