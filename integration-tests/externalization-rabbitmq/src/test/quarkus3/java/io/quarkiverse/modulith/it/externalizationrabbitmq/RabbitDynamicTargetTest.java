package io.quarkiverse.modulith.it.externalizationrabbitmq;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RabbitDynamicTargetTest {

    @Test
    void sendsExpressionTargetsThroughTheDefaultChannelWithTheTargetInTheHeaders() {
        // act
        given().queryParam("region", "eu").post("/orders/rerouted-1/rerouted").then().statusCode(204);

        // assert
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.routingKey == 'rerouted-1' }.exchange", contains("fallback"))
                .body("find { it.routingKey == 'rerouted-1' }.headers.quarkusModulith_routingTarget",
                        equalTo("eu-orders::rerouted-1"))
                .body("find { it.routingKey == 'rerouted-1' }.headers.springModulith_routingTarget",
                        equalTo("eu-orders::rerouted-1")));
    }
}
