package io.quarkiverse.modulith.it.moments;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * The week rule of a locale other than the build machine's, which a native executable only knows when the locale is
 * in {@code quarkus.locales}. With {@code de-DE}, weeks end on Sunday and the last days of 2026 and the first of 2027
 * form week 53 of 2026.
 */
@QuarkusTest
@TestProfile(GermanWeeks.class)
public class GermanWeeksTest {

    @ParameterizedTest
    @CsvSource({ "2026-11-15T23:30:00Z, 2026-11-15, 2026-W46", "2027-01-03T23:30:00Z, 2027-01-03, 2026-W53" })
    void weeksEndOnSunday(String clock, String day, String week) {
        // arrange
        given().body(clock).put("/time/clock").then().statusCode(200);
        given().delete("/billing/moments").then().statusCode(204);

        // act
        given().body("PT1H").post("/time/shift").then().statusCode(200);

        // assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> given().get("/billing/moments").then()
                .statusCode(200).body(equalTo(String.join("\n", "DayHasPassed[" + day + "]",
                        "HourHasPassed[" + day + "T23:00]", "WeekHasPassed[" + week + ", de-DE]"))));
    }
}
