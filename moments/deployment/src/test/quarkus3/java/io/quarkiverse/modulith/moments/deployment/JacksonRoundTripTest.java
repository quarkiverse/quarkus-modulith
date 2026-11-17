package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

class JacksonRoundTripTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MomentJson.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    ObjectMapper mapper;

    @ParameterizedTest
    @ValueSource(strings = { "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "SHIFTED_QUARTER_HAS_PASSED",
            "YEAR", "SHIFTED_QUARTER" })
    void writesTheMoment(String name) throws Exception {
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
    void readsTheMomentBack(String name) throws Exception {
        // arrange
        MomentJson moment = MomentJson.valueOf(name);
        Object expected = moment.moment();

        // act
        Object read = mapper.readValue(moment.json(), expected.getClass());

        // assert
        assertThat(read).isEqualTo(expected);
    }
}
