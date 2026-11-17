package io.quarkiverse.modulith.it.eventsjdbc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class JdbcRegistryTest {

    @BeforeEach
    void clear() {
        given().delete("/publications").then().statusCode(204);
    }

    @Test
    void storesAndCompletesPublications() {
        // arrange
        String id = given().body("hello").post("/publications/greeting.on-hello").then().statusCode(200)
                .extract().asString();

        // act
        given().post("/publications/" + id + "/completion").then().statusCode(204);

        // assert
        given().get("/publications/completed").then().statusCode(200).body("", contains("greeting.on-hello:hello"));
        given().get("/publications/incomplete").then().statusCode(200).body("", empty());
    }

    @Test
    void createsTheTableWithItsIndexes() {
        // act
        var response = given().get("/publications/indexes");

        // assert
        response.then().statusCode(200).body("", hasItems("event_publication_by_completion_date_idx",
                "event_publication_serialized_event_hash_idx"));
    }
}
