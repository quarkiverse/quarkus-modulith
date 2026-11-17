package io.quarkiverse.modulith.it.eventsneo4j;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class Neo4jRegistryTest {

    @BeforeEach
    void clear() {
        given().delete("/publications").then().statusCode(204);
    }

    @Test
    void storesFindsAndCompletesPublications() {
        // arrange
        String id = given().body("sku-1").post("/publications/catalog.on-listed").then().statusCode(200)
                .extract().asString();

        // act
        String found = given().get("/publications/by-event/catalog.on-listed/sku-1").then().statusCode(200)
                .extract().asString();
        given().post("/publications/" + id + "/completion").then().statusCode(204);

        // assert
        assertThat(found).isEqualTo(id);
        given().get("/publications/completed").then().statusCode(200).body("", contains("catalog.on-listed:sku-1"));
        given().get("/publications/incomplete").then().statusCode(200).body("", empty());
        given().get("/publications/by-event/catalog.on-listed/sku-1").then().statusCode(200).body(equalTo("none"));
    }
}
