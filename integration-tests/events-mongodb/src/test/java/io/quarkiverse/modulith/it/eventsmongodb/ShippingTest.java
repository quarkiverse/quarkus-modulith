package io.quarkiverse.modulith.it.eventsmongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ShippingTest {

    @BeforeEach
    void clear() {
        given().delete("/parcels").then().statusCode(204);
    }

    @Test
    void storesThePublicationWithTheParcel() {
        // act
        var response = given().post("/parcels/p-1");

        // assert
        response.then().statusCode(204);
        given().get("/parcels").then().statusCode(200).body("", contains("p-1"));
        given().get("/parcels/published").then().statusCode(200).body("", contains("p-1"));
    }

    @Test
    void rollsThePublicationBackWithTheParcel() {
        // act
        var response = given().queryParam("fail", true).post("/parcels/p-2");

        // assert
        response.then().statusCode(409).body(equalTo("Carrier unavailable for p-2"));
        given().get("/parcels").then().statusCode(200).body("", empty());
        given().get("/parcels/published").then().statusCode(200).body("", empty());
    }
}
