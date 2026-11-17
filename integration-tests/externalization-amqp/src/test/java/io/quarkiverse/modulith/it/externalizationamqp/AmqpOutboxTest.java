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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.it.broker.BrokerContainer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * The outbox mode is a build-time setting, so this profile test has no {@code @QuarkusIntegrationTest} twin. The
 * native outbox runs in {@code externalization-kafka-outbox}: the outbox sends through the same AMQP transport that
 * the native tests of this app cover in the direct mode.
 */
@QuarkusTest
@TestProfile(OutboxProfile.class)
public class AmqpOutboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @BeforeEach
    void reset() {
        given().delete("/audit").then().statusCode(204);
        given().delete("/publications").then().statusCode(204);
    }

    @Test
    void sendsThePlacedOrderFromTheOutbox() {
        // act
        given().queryParam("tenant", "acme").post("/orders/outbox-1/placed").then().statusCode(204);

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'outbox-1' }.address", contains("orders"))
                .body("find { it.subject == 'outbox-1' }.value", equalTo("{\"id\":\"outbox-1\",\"tenant\":\"acme\"}")));
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('outbox-1') }.status", contains("COMPLETED")));
    }

    @Test
    void keepsTheOutboxPublicationWhileAMQPIsDownAndSendsItOnceAMQPIsBack() {
        // arrange
        BrokerContainer amqp = BrokerContainer.publishing(given().get("/audit/broker").then().extract().asString());

        // act
        amqp.pause();
        try {
            given().queryParam("tenant", "acme").post("/orders/outbox-outage-1/placed").then().statusCode(204);
            await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/incomplete").then()
                    .body("find { it.event.contains('outbox-outage-1') }.completionAttempts",
                            greaterThanOrEqualTo(2)));
            given().get("/audit/received").then().body("findAll { it.subject == 'outbox-outage-1' }", empty());
        } finally {
            amqp.unpause();
        }

        // assert
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/publications/completed").then()
                .body("findAll { it.event.contains('outbox-outage-1') }.status", contains("COMPLETED")));
        given().get("/publications/incomplete").then()
                .body("findAll { it.event.contains('outbox-outage-1') }", empty());
        await().atMost(TIMEOUT).untilAsserted(() -> given().get("/audit/received").then()
                .body("findAll { it.subject == 'outbox-outage-1' }", hasSize(greaterThanOrEqualTo(1)))
                .body("findAll { it.subject == 'outbox-outage-1' }.address", everyItem(equalTo("orders"))));
    }
}
