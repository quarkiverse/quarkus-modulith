package io.quarkiverse.modulith.flyway.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.flywaydb.core.api.Location;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModulithFlywayCustomizerTest {

    @ParameterizedTest
    @CsvSource({ "db/migration, false", "db/*, true", "db/**/migration, true", "db/v?, true" })
    void findsWildcardLocations(String path, boolean wildcard) {
        // arrange
        Location[] locations = { Location.fromPath("classpath:", path) };

        // act
        boolean found = !ModulithFlywayCustomizer.wildcards(locations).isEmpty();

        // assert
        assertThat(found).isEqualTo(wildcard);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "unset", value = { "unset, unset, false", "true, 0, false", "false, unset, true",
            "unset, 5, true", "true, 5, true", "false, 0, true" })
    void detectsBaselineSettingsTheModuleMigrationsOverride(Boolean onMigrate, String version, boolean overridden) {
        // act
        boolean result = ModulithFlywayCustomizer.overridesTheBaseline(Optional.ofNullable(onMigrate),
                Optional.ofNullable(version));

        // assert
        assertThat(result).isEqualTo(overridden);
    }
}
