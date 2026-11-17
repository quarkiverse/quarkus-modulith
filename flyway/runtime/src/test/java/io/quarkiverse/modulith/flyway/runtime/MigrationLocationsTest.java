package io.quarkiverse.modulith.flyway.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;

class MigrationLocationsTest {

    @Test
    void appendsTheModuleFolderToEveryLocation() {
        // arrange
        Location[] locations = { Location.fromPath("classpath:", "db/migration"),
                Location.fromPath("filesystem:", "/opt/migrations") };

        // act
        String[] moduleLocations = MigrationLocations.in(locations, "order_internal");

        // assert
        assertThat(moduleLocations).containsExactly("classpath:db/migration/order_internal",
                "filesystem:/opt/migrations/order_internal");
    }
}
