package io.quarkiverse.modulith.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class OutboxRecoveryTest {

    private static final String CONFIRMATION_STATUS = "find { it.listenerId == 'notification.order-confirmation' }.status";

    @AfterEach
    void endOutage() {
        given().post("/notifications/outage?enabled=false").then().statusCode(204);
    }

    @Test
    void failedDeliveryIsKeptAndCompletesWhenResubmitted() {
        // arrange
        given().post("/notifications/outage?enabled=true").then().statusCode(204);

        // act
        String orderId = given().contentType(ContentType.JSON)
                .body("{\"sku\":\"sku-1\",\"quantity\":1}")
                .post("/orders")
                .then().statusCode(200)
                .extract().path("id");
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> given().get("/modulith/publications/incomplete")
                .then().body(CONFIRMATION_STATUS, equalTo("FAILED")));
        String notifiedDuringOutage = given().get("/notifications/" + orderId).then().extract().asString();
        given().post("/notifications/outage?enabled=false").then().statusCode(204);
        given().post("/modulith/publications/resubmit").then().statusCode(200);

        // assert
        assertThat(notifiedDuringOutage).isEqualTo("false");
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            given().get("/notifications/" + orderId).then().body(equalTo("true"));
            given().get("/modulith/publications/incomplete").then().body("$", empty());
        });
    }
}
