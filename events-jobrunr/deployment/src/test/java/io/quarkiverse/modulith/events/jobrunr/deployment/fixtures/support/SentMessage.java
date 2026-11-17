package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import io.quarkiverse.modulith.events.RoutingTarget;

public record SentMessage(Object payload, RoutingTarget target) {
}
