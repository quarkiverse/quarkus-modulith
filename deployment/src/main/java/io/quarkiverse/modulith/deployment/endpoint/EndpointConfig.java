package io.quarkiverse.modulith.deployment.endpoint;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The {@code /q/modulith} endpoint, the counterpart of Spring Modulith's {@code /actuator/modulith}.
 */
@ConfigMapping(prefix = "quarkus.modulith.endpoint")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface EndpointConfig {

    /**
     * Whether to serve the module model as JSON. Needs an HTTP extension such as {@code quarkus-vertx-http} or
     * {@code quarkus-rest}. When not set, the endpoint is served in dev and test mode only: the JSON maps the internals
     * of the application, so a packaged application serves it only with {@code true}, and should then protect the path
     * or move it to the management interface.
     */
    Optional<Boolean> enabled();

    /**
     * The path of the endpoint, relative to the non-application root path ({@code /q}), or to the management
     * interface root when the management interface is enabled.
     */
    @WithDefault("modulith")
    String path();
}
