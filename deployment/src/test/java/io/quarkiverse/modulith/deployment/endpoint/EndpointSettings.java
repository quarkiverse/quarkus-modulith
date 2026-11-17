package io.quarkiverse.modulith.deployment.endpoint;

import java.util.Optional;

record EndpointSettings(Optional<Boolean> enabled, String path) implements EndpointConfig {
}
