package io.quarkiverse.modulith.core.fixtures.shop.catalog.internal;

import io.quarkiverse.modulith.NamedInterface;

@NamedInterface(name = "spi", propagate = false)
public interface Spi {
    Stock stock();
}
