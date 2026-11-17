package io.quarkiverse.modulith.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ShipmentRecoveryTest {

    @AfterEach
    void endOutage() {
        given().post("/notifications/outage?enabled=false").then().statusCode(204);
    }

    @Test
    void storesAndReadsBackASubtypeOfTheListenedType() {
        // arrange
        given().post("/notifications/outage?enabled=true").then().statusCode(204);

        // act
        given().contentType(ContentType.JSON).body("{\"street\":\"1 Main St\",\"city\":\"Lisbon\"}")
                .post("/orders/o-ship/shipment").then().statusCode(204);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> given().get("/modulith/publications/incomplete")
                .then().body("eventType", hasItem("io.quarkiverse.modulith.it.order.events.OrderShipped"))
                .body("status", hasItem("FAILED")));
        given().post("/notifications/outage?enabled=false").then().statusCode(204);
        given().post("/modulith/publications/resubmit").then().statusCode(200).body(equalTo("1"));

        // assert
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            given().get("/notifications/shipped-o-ship").then().body(equalTo("true"));
            given().get("/modulith/publications/incomplete").then().body("$", empty());
        });
    }
}
