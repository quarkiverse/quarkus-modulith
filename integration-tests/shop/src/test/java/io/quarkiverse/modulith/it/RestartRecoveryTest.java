package io.quarkiverse.modulith.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CrashedPublicationsProfile.class)
public class RestartRecoveryTest {

    @Test
    void resubmitsWhatTheCrashedInstanceLeftAfterStartup() {
        // arrange
        Duration timeout = Duration.ofSeconds(20);

        // act
        await().atMost(timeout).untilAsserted(() -> given().get("/modulith/publications/incomplete").then()
                .body("listenerId", contains("removed.listener")).body("status", contains("FAILED")));

        // assert
        given().get("/notifications/crashed-processing").then().body(equalTo("true"));
        given().get("/notifications/crashed-failed").then().body(equalTo("true"));
        given().get("/notifications/crashed-published").then().body(equalTo("true"));
        given().get("/modulith/publications/completed").then().body(equalTo("3"));
    }
}
