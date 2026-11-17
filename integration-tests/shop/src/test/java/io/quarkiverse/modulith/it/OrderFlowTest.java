package io.quarkiverse.modulith.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class OrderFlowTest {

    @Test
    void modulesReactToAPlacedOrder() {
        // arrange
        int stockBefore = Integer.parseInt(given().get("/inventory/sku-1").then().statusCode(200).extract().asString());

        // act
        String orderId = given().contentType(ContentType.JSON)
                .body("{\"sku\":\"sku-1\",\"quantity\":3}")
                .post("/orders")
                .then().statusCode(200)
                .extract().path("id");

        // assert
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            given().get("/inventory/sku-1").then().body(equalTo(String.valueOf(stockBefore - 3)));
            given().get("/notifications/" + orderId).then().body(equalTo("true"));
            given().get("/modulith/publications/incomplete").then().body("$", empty());
        });
    }

    @Test
    void exposesTheModuleModel() {
        // arrange
        String path = "/modulith/modules";

        // act
        var response = given().get(path);

        // assert
        response.then().statusCode(200).body("name", hasItems("common", "inventory", "notification", "order"));
    }

    @Test
    void reportsNoViolations() {
        // act
        var response = given().get("/modulith/violations");

        // assert
        response.then().statusCode(200).body(equalTo("0"));
    }
}
