package io.quarkiverse.modulith.events.jdbc.runtime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class JdbcOperationsTest {

    @Test
    void bindsOnlyOptionalInstants() {
        // act & assert
        assertThatThrownBy(() -> JdbcOperations.bind(null, Optional.of("2026-01-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only an Optional of an Instant can be bound, not one of java.lang.String");
    }
}
