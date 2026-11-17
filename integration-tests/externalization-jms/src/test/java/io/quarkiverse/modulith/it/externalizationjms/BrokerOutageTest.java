package io.quarkiverse.modulith.it.externalizationjms;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@WithTestResource(value = ArtemisBroker.class, initArgs = @ResourceArg(name = ArtemisBroker.CALL_TIMEOUT, value = "2000"))
class BrokerOutageTest {

    BrokerControl broker;

    @Test
    void keepsThePublicationIncompleteUntilItIsResubmittedToTheRecoveredBroker() {
        // arrange
        broker.pause();
        try {
            Orders.place("o-9");
            await().atMost(Orders.TIMEOUT).until(() -> Orders.statuses("o-9").equals(List.of("FAILED")));
        } finally {
            broker.resume();
        }

        // act
        String resubmitted = given().post("/publications/resubmit").then().statusCode(200).extract().asString();

        // assert
        assertThat(resubmitted).isEqualTo("1");
        JsonPath message = Orders.receive("o-9");
        assertThat(message.getString("text")).isEqualTo(Orders.json("o-9"));
        await().atMost(Orders.TIMEOUT).until(() -> Orders.statuses("o-9").equals(List.of("COMPLETED")));
        await().atMost(Orders.TIMEOUT).until(() -> Orders.brokerResults("o-9").contains(message.getString("messageId")));
    }
}
