package io.quarkiverse.modulith.it.externalizationjobrunr;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class JobRunrExternalizationTest {

    @BeforeEach
    void reset() {
        await().atMost(Shop.TIMEOUT).until(() -> Shop.jobs().get("pending") == 0);
        given().delete("/deliveries").then().statusCode(204);
    }

    @Test
    void runsOneJobPerTransportAfterTheCommitAndDeliversOnce() {
        // arrange
        int succeeded = Shop.jobs().get("succeeded");

        // act
        Shop.place("o-1");

        // assert
        await().atMost(Shop.TIMEOUT).until(() -> Shop.jobs().get("succeeded") == succeeded + 2);
        assertThat(Shop.deliveries("warehouse")).isEqualTo(Shop.delivered("warehouse", "o-1"));
        assertThat(Shop.deliveries("broker")).isEqualTo(Shop.delivered("orders", "o-1"));
        assertThat(Shop.attempts("warehouse")).isEqualTo(1);
        assertThat(Shop.attempts("broker")).isEqualTo(1);
        await().atMost(Shop.TIMEOUT).until(() -> Shop.externalized().equals("[\"ack-1\"]"));
    }

    @Test
    void enqueuesNothingWhenTheTransactionRollsBack() {
        // arrange
        Map<String, Integer> before = Shop.jobs();

        // act
        given().contentType(ContentType.JSON).body(Shop.json("o-2")).post("/orders/rolled-back").then()
                .statusCode(409);

        // assert
        assertThat(Shop.jobs()).isEqualTo(before);
        assertThat(Shop.deliveries("warehouse")).isEqualTo("[]");
        assertThat(Shop.deliveries("broker")).isEqualTo("[]");
    }

    @Test
    void retriesAFailingTransportUntilItSucceeds() {
        // arrange
        Shop.failNext("warehouse", 2);

        // act
        Shop.place("o-3");

        // assert
        await().atMost(Shop.TIMEOUT).until(() -> Shop.deliveries("warehouse").equals(Shop.delivered("warehouse", "o-3")));
        assertThat(Shop.attempts("warehouse")).isEqualTo(3);
        assertThat(Shop.deliveries("broker")).isEqualTo(Shop.delivered("orders", "o-3"));
    }

    @Test
    void retriesWhileTheBrokerIsDown() {
        // arrange
        Shop.failNext("broker", 1);

        // act
        Shop.place("o-4");

        // assert
        await().atMost(Shop.TIMEOUT).until(() -> Shop.deliveries("broker").equals(Shop.delivered("orders", "o-4")));
        assertThat(Shop.attempts("broker")).isEqualTo(2);
        await().atMost(Shop.TIMEOUT).until(() -> Shop.externalized().equals("[\"ack-1\"]"));
        assertThat(Shop.deliveries("warehouse")).isEqualTo(Shop.delivered("warehouse", "o-4"));
    }

    @Test
    void rejectsAnUnknownTransport() {
        // act
        int status = given().get("/deliveries/brokr").then().extract().statusCode();

        // assert
        assertThat(status).isEqualTo(404);
    }
}
