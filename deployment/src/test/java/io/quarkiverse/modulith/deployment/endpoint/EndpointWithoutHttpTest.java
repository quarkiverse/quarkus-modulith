package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

class EndpointWithoutHttpTest {

    @Test
    void declaresNoRouteWithoutAnHttpServer() {
        // arrange
        List<RouteBuildItem> routes = new ArrayList<>();

        // act
        new EndpointRecordingSteps().route(null, null, null, new Capabilities(Set.of()), routes::add);

        // assert
        assertThat(routes).isEmpty();
    }
}
