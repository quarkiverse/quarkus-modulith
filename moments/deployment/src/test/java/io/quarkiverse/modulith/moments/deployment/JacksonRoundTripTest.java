package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.json.JsonMapper;

class JacksonRoundTripTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MomentJson.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    JsonMapper mapper;

    @ParameterizedTest
    @ValueSource(strings = { "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "SHIFTED_QUARTER_HAS_PASSED",
            "YEAR", "SHIFTED_QUARTER" })
    void writesTheMoment(String name) {
        // arrange
        MomentJson moment = MomentJson.valueOf(name);

        // act
        String written = mapper.writeValueAsString(moment.moment());

        // assert
        assertThat(written).isEqualTo(moment.json());
    }

    @ParameterizedTest
    @ValueSource(strings = { "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "SHIFTED_QUARTER_HAS_PASSED",
            "YEAR", "SHIFTED_QUARTER" })
    void readsTheMomentBack(String name) {
        // arrange
        MomentJson moment = MomentJson.valueOf(name);
        Object expected = moment.moment();

        // act
        Object read = mapper.readValue(moment.json(), expected.getClass());

        // assert
        assertThat(read).isEqualTo(expected);
    }

    /**
     * Publications stored by an application on Quarkus 3 (Jackson 2) are read after its upgrade to Quarkus 4.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "WEEK|{\"year\":\"2026\",\"week\":46,\"locale\":\"en_US\",\"startDate\":\"2026-11-08\",\"endDate\":\"2026-11-14\"}",
            "QUARTER|{\"year\":\"2026\",\"quarter\":{\"quarter\":\"Q1\",\"startMonth\":\"JANUARY\"}}",
            "SHIFTED_QUARTER_HAS_PASSED|{\"year\":\"2026\",\"quarter\":{\"quarter\":\"Q3\",\"startMonth\":\"FEBRUARY\"}}",
            "YEAR|{\"year\":\"2026\"}",
            "SHIFTED_QUARTER|{\"quarter\":\"Q4\",\"startMonth\":\"APRIL\"}" })
    void readsTheQuarkus3Form(String name, String json) {
        // arrange
        Object expected = MomentJson.valueOf(name).moment();

        // act
        Object read = mapper.readValue(json, expected.getClass());

        // assert
        assertThat(read).isEqualTo(expected);
    }
}
