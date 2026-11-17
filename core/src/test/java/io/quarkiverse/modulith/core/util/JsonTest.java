package io.quarkiverse.modulith.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.DependencyType;

class JsonTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '\'', value = { "plain|\"plain\"", "a\"b|\"a\\\"b\"",
            "a\\b|\"a\\\\b\"", "a/b|\"a/b\"", "ünï|\"ünï\"" })
    void quotesStrings(String value, String quoted) {
        // act
        String result = Json.quote(value);

        // assert
        assertThat(result).isEqualTo(quoted);
    }

    @ParameterizedTest
    @CsvSource({ "10,\\n", "13,\\r", "9,\\t", "0,\\u0000", "1,\\u0001", "31,\\u001f", "8,\\u0008", "12,\\u000c" })
    void escapesControlCharacters(int character, String escaped) {
        // act
        String result = Json.quote("a" + (char) character + "b");

        // assert
        assertThat(result).isEqualTo("\"a" + escaped + "b\"");
    }

    @Test
    void rendersEmptyMapsAndCollections() {
        // act
        String result = Json.toString(List.of(Map.of(), List.of()));

        // assert
        assertThat(result).isEqualTo("[ {}, [] ]");
    }

    @Test
    void rendersMapsEnumsAndOtherValues() {
        // act
        String result = Json.toString(Map.of("kind", DependencyType.ENTITY, "count", 2, "flag", true).entrySet()
                .stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left,
                        LinkedHashMap::new)));

        // assert
        assertThat(result).isEqualTo("{ \"count\" : 2, \"flag\" : true, \"kind\" : \"ENTITY\" }");
    }

    @ParameterizedTest
    @CsvSource({ "32", "127" })
    void keepsPrintableCharacters(int character) {
        // act
        String result = Json.quote(String.valueOf((char) character));

        // assert
        assertThat(result).isEqualTo("\"" + (char) character + "\"");
    }
}
