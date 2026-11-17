package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order;

import java.util.List;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders")
public record OrderPlaced(String id, List<String> items) {
}
