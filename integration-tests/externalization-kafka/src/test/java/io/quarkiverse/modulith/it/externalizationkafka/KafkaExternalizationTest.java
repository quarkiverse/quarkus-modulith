package io.quarkiverse.modulith.it.externalizationkafka;

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
public class KafkaExternalizationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @BeforeEach
    void reset() {
        given().delete("/audit").then().statusCode(204);
        given().delete("/publications").then().statusCode(204);
    }

    @Test
    void sendsThePlacedOrderAsJsonWithTopicKeyAndHeaders() {
        // act
        given().queryParam("tenant", "acme").post("/orders/placed-1/placed").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.key == 'placed-1' }.topic", contains("orders"))
                .body("find { it.key == 'placed-1' }.value", equalTo("{\"id\":\"placed-1\",\"tenant\":\"acme\"}"))
                .body("find { it.key == 'placed-1' }.headers", equalTo(Map.of("tenant", "acme"))));
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('placed-1') }.status", contains("COMPLETED"))
                .body("findAll { it.event.contains('placed-1') }.completionAttempts", contains(1)));
    }

    @Test
    void reportsTheKafkaRecordMetadataInEventExternalized() {
        // act
        given().queryParam("tenant", "acme").post("/orders/metadata-1/placed").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/externalized").then()
                .body("findAll { it.target == 'orders::metadata-1' }.topic", contains("orders"))
                .body("findAll { it.target == 'orders::metadata-1' }.partition", contains(0))
                .body("find { it.target == 'orders::metadata-1' }.offset", greaterThanOrEqualTo(0)));
    }

    @Test
    void routesTheSpringKeyExpressionToTheShipmentsTopic() {
        // act
        given().post("/orders/shipped-1/shipped").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.key == 'shipped-1' }.topic", contains("shipments"))
                .body("find { it.key == 'shipped-1' }.value", equalTo("{\"id\":\"shipped-1\"}"))
                .body("find { it.key == 'shipped-1' }.headers", equalTo(Map.of())));
    }

    @Test
    void sendsExpressionTargetsThroughTheDefaultChannelToTheirTopic() {
        // act
        given().queryParam("region", "eu").post("/orders/rerouted-1/rerouted").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.key == 'rerouted-1' }.topic", contains("eu-orders")));
    }

    @Test
    void keepsThePublicationFailedWhileKafkaIsDownAndDeliversItWhenRedelivered() {
        // arrange
        BrokerContainer kafka = BrokerContainer.publishing(given().get("/audit/broker").then().extract().asString());

        // act
        kafka.pause();
        try {
            given().queryParam("tenant", "acme").post("/orders/outage-1/placed").then().statusCode(204);
            await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/incomplete").then()
                    .body("findAll { it.event.contains('outage-1') }.status", contains("FAILED")));
        } finally {
            kafka.unpause();
        }
        String id = given().get("/publications/incomplete").then().extract()
                .path("find { it.event.contains('outage-1') }.id");
        given().post("/publications/" + id + "/redeliver").then().statusCode(200).body(equalTo("true"));

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('outage-1') }.status", contains("COMPLETED")));
        given().get("/publications/incomplete").then().body("findAll { it.event.contains('outage-1') }", empty());
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.key == 'outage-1' }", hasSize(greaterThanOrEqualTo(1)))
                .body("findAll { it.key == 'outage-1' }.topic", everyItem(equalTo("orders"))));
    }
}
