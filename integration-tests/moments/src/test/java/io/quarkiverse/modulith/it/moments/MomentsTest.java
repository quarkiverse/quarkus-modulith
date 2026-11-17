package io.quarkiverse.modulith.it.moments;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Sets the application's clock, shifts time with the time machine and checks the moments the billing module's
 * listeners received. The application uses {@code en-US} weeks (Sunday to Saturday) and fiscal years starting in
 * October. Only uses HTTP so that {@link MomentsIT} runs it against the packaged application.
 */
@QuarkusTest
public class MomentsTest {

    static Stream<Arguments> shifts() {
        return Stream.of(
                arguments("2026-09-30T22:30:00Z", "PT2H", "2026-10-01T00:30", String.join("\n",
                        "DayHasPassed[2026-09-30]",
                        "HourHasPassed[2026-09-30T22:00]",
                        "HourHasPassed[2026-09-30T23:00]",
                        "MonthHasPassed[2026-09]",
                        "QuarterHasPassed[2025 Q4/OCTOBER]")),
                arguments("2026-11-14T23:30:00Z", "PT1H", "2026-11-15T00:30", String.join("\n",
                        "DayHasPassed[2026-11-14]",
                        "HourHasPassed[2026-11-14T23:00]",
                        "WeekHasPassed[2026-W46, en-US]")),
                arguments("2026-12-31T23:00:00Z", "PT1H", "2027-01-01T00:00", String.join("\n",
                        "DayHasPassed[2026-12-31]",
                        "HourHasPassed[2026-12-31T23:00]",
                        "MonthHasPassed[2026-12]",
                        "QuarterHasPassed[2026 Q1/OCTOBER]",
                        "YearHasPassed[2026]")),
                arguments("2027-01-02T12:00:00Z", "P1D", "2027-01-03T12:00", String.join("\n",
                        "DayHasPassed[2027-01-02]",
                        "HourHasPassed[2027-01-02T12:00]",
                        "HourHasPassed[2027-01-02T13:00]",
                        "HourHasPassed[2027-01-02T14:00]",
                        "HourHasPassed[2027-01-02T15:00]",
                        "HourHasPassed[2027-01-02T16:00]",
                        "HourHasPassed[2027-01-02T17:00]",
                        "HourHasPassed[2027-01-02T18:00]",
                        "HourHasPassed[2027-01-02T19:00]",
                        "HourHasPassed[2027-01-02T20:00]",
                        "HourHasPassed[2027-01-02T21:00]",
                        "HourHasPassed[2027-01-02T22:00]",
                        "HourHasPassed[2027-01-02T23:00]",
                        "HourHasPassed[2027-01-03T00:00]",
                        "HourHasPassed[2027-01-03T01:00]",
                        "HourHasPassed[2027-01-03T02:00]",
                        "HourHasPassed[2027-01-03T03:00]",
                        "HourHasPassed[2027-01-03T04:00]",
                        "HourHasPassed[2027-01-03T05:00]",
                        "HourHasPassed[2027-01-03T06:00]",
                        "HourHasPassed[2027-01-03T07:00]",
                        "HourHasPassed[2027-01-03T08:00]",
                        "HourHasPassed[2027-01-03T09:00]",
                        "HourHasPassed[2027-01-03T10:00]",
                        "HourHasPassed[2027-01-03T11:00]",
                        "WeekHasPassed[2027-W1, en-US]")));
    }

    @ParameterizedTest
    @MethodSource("shifts")
    void listenersReceiveEveryMomentOnTheWay(String clock, String shift, String now, String expected) {
        // arrange
        given().body(clock).put("/time/clock").then().statusCode(200);
        given().delete("/billing/moments").then().statusCode(204);

        // act
        String shifted = given().body(shift).post("/time/shift").then().statusCode(200).extract().asString();

        // assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> given().get("/billing/moments").then().statusCode(200).body(equalTo(expected)));
        given().get("/time/now").then().body(equalTo(now));
        assertThat(shifted).isEqualTo(now);
    }
}
