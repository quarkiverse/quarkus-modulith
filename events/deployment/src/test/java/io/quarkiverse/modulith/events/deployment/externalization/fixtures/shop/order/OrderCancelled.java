package io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized
public record OrderCancelled(String id) {
}
