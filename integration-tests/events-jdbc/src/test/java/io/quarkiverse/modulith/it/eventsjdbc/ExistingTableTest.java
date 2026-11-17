package io.quarkiverse.modulith.it.eventsjdbc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ExistingTableProfile.class)
class ExistingTableTest {

    @Test
    void leavesTheExistingTableAsItIs() {
        // act
        var indexes = given().get("/publications/indexes");

        // assert
        indexes.then().statusCode(200).body("", contains("event_publication_pkey"));
        given().get("/publications/incomplete").then().statusCode(200).body("", contains("existing.listener:kept"));
    }
}
