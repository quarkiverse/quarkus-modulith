package io.quarkiverse.modulith.it.externalizationrabbitmq;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RabbitDynamicTargetTest {

    @Test
    void sendsExpressionTargetsThroughTheDefaultChannelToTheirExchange() {
        // act
        given().queryParam("region", "eu").post("/orders/rerouted-1/rerouted").then().statusCode(204);

        // assert
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.routingKey == 'rerouted-1' }.exchange", contains("eu-orders")));
    }
}
