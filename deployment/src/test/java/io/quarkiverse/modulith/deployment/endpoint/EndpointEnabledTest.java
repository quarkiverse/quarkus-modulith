package io.quarkiverse.modulith.deployment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.runtime.LaunchMode;

class EndpointEnabledTest {

    @ParameterizedTest
    @CsvSource(nullValues = "unset", value = {
            "unset, NORMAL, false",
            "unset, DEVELOPMENT, true",
            "unset, TEST, true",
            "true, NORMAL, true",
            "false, DEVELOPMENT, false",
            "false, TEST, false" })
    void servesTheEndpointInProductionOnlyWhenEnabled(Boolean enabled, LaunchMode launchMode, boolean expected) {
        // arrange
        EndpointEnabled condition = new EndpointEnabled(new EndpointSettings(Optional.ofNullable(enabled), "modulith"),
                launchMode);

        // act
        boolean actual = condition.getAsBoolean();

        // assert
        assertThat(actual).isEqualTo(expected);
    }
}
