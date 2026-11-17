package io.quarkiverse.modulith.events.jms.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JmsPropertiesTest {

    @Test
    void keepsTheJmsTypesAndSendsOtherValuesAsText() {
        // arrange
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("flag", true);
        headers.put("aByte", (byte) 1);
        headers.put("aShort", (short) 2);
        headers.put("anInt", 3);
        headers.put("aLong", 4L);
        headers.put("aFloat", 5.5f);
        headers.put("aDouble", 6.5d);
        headers.put("text", "seven");
        headers.put("amount", new BigDecimal("8.25"));
        headers.put("id", UUID.fromString("00000000-0000-0000-0000-000000000009"));
        headers.put("missing", null);

        // act
        Map<String, Object> properties = JmsProperties.of(headers);

        // assert
        assertThat(properties).containsExactly(Map.entry("flag", true), Map.entry("aByte", (byte) 1),
                Map.entry("aShort", (short) 2), Map.entry("anInt", 3), Map.entry("aLong", 4L),
                Map.entry("aFloat", 5.5f), Map.entry("aDouble", 6.5d), Map.entry("text", "seven"),
                Map.entry("amount", "8.25"), Map.entry("id", "00000000-0000-0000-0000-000000000009"));
    }

    @Test
    void returnsNoPropertiesForNoHeaders() {
        // act
        Map<String, Object> properties = JmsProperties.of(Map.of());

        // assert
        assertThat(properties).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "orderId", "_private", "$amount", "JMSXGroupID", "a1", "x" })
    void acceptsJavaIdentifiers(String name) {
        // act
        boolean valid = JmsProperties.isValidName(name);

        // assert
        assertThat(valid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1st", "order-id", "order id", "NULL", "true", "False", "not", "AND", "or",
            "Between", "LIKE", "in", "IS", "escape" })
    void rejectsOtherNames(String name) {
        // act
        boolean valid = JmsProperties.isValidName(name);

        // assert
        assertThat(valid).isFalse();
    }

    @Test
    void failsOnAnInvalidHeaderName() {
        // arrange
        Map<String, Object> headers = Map.of("order-id", "o-1");

        // act & assert
        assertThatThrownBy(() -> JmsProperties.of(headers)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The header order-id is not a valid JMS property name: use a Java identifier that is not"
                        + " a message selector keyword");
    }
}
