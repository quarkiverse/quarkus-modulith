package io.quarkiverse.modulith.events.deployment.externalization.fixtures.support;

import java.util.Map;

import io.quarkiverse.modulith.events.RoutingTarget;

public record SentMessage(Object payload, RoutingTarget target, Map<String, Object> headers) {
}
