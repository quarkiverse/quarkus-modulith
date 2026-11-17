package io.quarkiverse.modulith.it.externalizationamqp;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.it.broker.BrokerContainer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmqpExternalizationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @BeforeEach
    void reset() {
        given().delete("/audit").then().statusCode(204);
        given().delete("/publications").then().statusCode(204);
    }

    @Test
    void sendsThePlacedOrderAsJsonWithAddressSubjectAndProperties() {
        // act
        given().queryParam("tenant", "acme").post("/orders/placed-1/placed").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'placed-1' }.address", contains("orders"))
                .body("find { it.subject == 'placed-1' }.value", equalTo("{\"id\":\"placed-1\",\"tenant\":\"acme\"}"))
                .body("find { it.subject == 'placed-1' }.properties", equalTo(Map.of("tenant", "acme")))
                .body("find { it.subject == 'placed-1' }.contentType", equalTo("application/json")));
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('placed-1') }.status", contains("COMPLETED"))
                .body("findAll { it.event.contains('placed-1') }.completionAttempts", contains(1)));
    }

    @Test
    void firesEventExternalizedOnceTheBrokerAccepted() {
        // act
        given().queryParam("tenant", "acme").post("/orders/metadata-1/placed").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/externalized").then()
                .body("findAll { it.target == 'orders::metadata-1' }.brokerResultType", contains("none")));
    }

    @Test
    void routesTheSpringKeyExpressionToTheShipmentsAddress() {
        // act
        given().post("/orders/shipped-1/shipped").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'shipped-1' }.address", contains("shipments"))
                .body("find { it.subject == 'shipped-1' }.value", equalTo("{\"id\":\"shipped-1\"}"))
                .body("find { it.subject == 'shipped-1' }.properties", equalTo(Map.of())));
    }

    @Test
    void sendsExpressionTargetsThroughTheDefaultChannelToTheirAddress() {
        // act
        given().queryParam("region", "eu").post("/orders/rerouted-1/rerouted").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'rerouted-1' }.address", contains("eu-orders")));
    }

    @Test
    void keepsThePublicationFailedWhileAMQPIsDownAndDeliversItWhenRedelivered() {
        // arrange
        BrokerContainer amqp = BrokerContainer.publishing(given().get("/audit/broker").then().extract().asString());

        // act
        amqp.pause();
        try {
            given().queryParam("tenant", "acme").post("/orders/outage-1/placed").then().statusCode(204);
            await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/incomplete").then()
                    .body("findAll { it.event.contains('outage-1') }.status", contains("FAILED")));
        } finally {
            amqp.unpause();
        }
        String id = given().get("/publications/incomplete").then().extract()
                .path("find { it.event.contains('outage-1') }.id");
        given().post("/publications/" + id + "/redeliver").then().statusCode(200).body(equalTo("true"));

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('outage-1') }.status", contains("COMPLETED")));
        given().get("/publications/incomplete").then().body("findAll { it.event.contains('outage-1') }", empty());
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'outage-1' }", hasSize(greaterThanOrEqualTo(1)))
                .body("findAll { it.subject == 'outage-1' }.address", everyItem(equalTo("orders"))));
    }
}
