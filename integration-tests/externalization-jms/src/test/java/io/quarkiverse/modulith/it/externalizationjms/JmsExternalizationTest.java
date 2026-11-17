package io.quarkiverse.modulith.it.externalizationjms;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@WithTestResource(ArtemisBroker.class)
class JmsExternalizationTest {

    @Test
    void sendsTheEventAsJsonToTheQueueAfterTheCommit() {
        // act
        Orders.place("o-1");

        // assert
        JsonPath message = Orders.receive("o-1");
        assertThat(message.getString("text")).isEqualTo(Orders.json("o-1"));
        assertThat(message.getString("destination")).isEqualTo("ActiveMQQueue[orders]");
        assertThat(message.getMap("properties")).containsExactly(entry("lineCount", 2), entry("orderId", "o-1"));
        String messageId = message.getString("messageId");
        assertThat(messageId).startsWith("ID:");
        await().atMost(Orders.TIMEOUT).until(() -> Orders.statuses("o-1").equals(List.of("COMPLETED")));
        await().atMost(Orders.TIMEOUT).until(() -> Orders.brokerResults("o-1").contains(messageId));
    }

    @Test
    void sendsNothingWhenTheTransactionRollsBack() {
        // act
        given().contentType(ContentType.JSON).body(Orders.json("o-2")).post("/orders/rolled-back").then()
                .statusCode(409);

        // assert
        assertThat(Orders.receiveStatus("o-2", 1000)).isEqualTo(204);
    }

    @Test
    void rejectsAnOrderIdThatWouldBreakTheMessageSelector() {
        // act
        int status = given().get("/messages/orders/o'3").then().extract().statusCode();

        // assert
        assertThat(status).isEqualTo(400);
    }
}
