package io.quarkiverse.modulith.runtime.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InitializerOrderTest {

    @ParameterizedTest
    @CsvSource({ "java.lang.String, 0", "java.lang.Integer, 1", "java.lang.Long, 2" })
    void placesKnownInitializersInBuildOrderAndUnknownOnesLast(Class<?> beanClass, int expected) {
        // arrange
        InitializerOrder order = new InitializerOrder(List.of(String.class.getName(), Integer.class.getName()));

        // act
        int index = order.indexOf(beanClass);

        // assert
        assertThat(index).isEqualTo(expected);
    }
}
