package io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized
public record OrderAudited(String id) {
}
