package io.quarkiverse.modulith.it.flyway.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(BillingModuleProfile.class)
class BillingModuleTest {

    @Test
    void migratesOnlyTheStartedModules() {
        // act
        var log = given().get("/migrations/log");
        var tables = given().get("/migrations/history-tables");

        // assert
        log.then().statusCode(200).body("", contains("root", "catalog", "billing"));
        tables.then().statusCode(200).body("", contains("flyway_schema_history", "flyway_schema_history_billing",
                "flyway_schema_history_catalog"));
    }
}
