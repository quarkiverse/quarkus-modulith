package io.quarkiverse.modulith.it.flyway;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ModuleMigrationsTest {

    @Test
    void migratesTheModulesInDependencyOrder() {
        // act
        var log = given().get("/migrations/log");

        // assert
        log.then().statusCode(200).body("", contains("root", "catalog", "billing", "shipping"));
    }

    @Test
    void keepsAHistoryTablePerModuleWithMigrations() {
        // act
        var tables = given().get("/migrations/history-tables");

        // assert
        tables.then().statusCode(200).body("", contains("flyway_schema_history", "flyway_schema_history_billing",
                "flyway_schema_history_catalog", "flyway_schema_history_shipping"));
    }
}
