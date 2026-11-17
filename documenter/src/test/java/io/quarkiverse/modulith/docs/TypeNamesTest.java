package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TypeNamesTest {

    @ParameterizedTest
    @CsvSource({
            "com.acme.order.Order, , c.a.o.Order",
            "Order, , Order",
            "com.acme.order.Order$Line, '', c.a.o.Order.Line",
            "com.acme.order.Order, com.acme.order, c.a.o.Order",
            "com.acme.order.internal.Repo, com.acme.order, c.a.o.internal.Repo",
            "com.other.Thing$Inner, com.acme.order, com.other.Thing.Inner",
            "com.acme.orderly.Thing, com.acme.order, c.a.oly.Thing"
    })
    void abbreviatesRelativeToTheBasePackage(String type, String basePackage, String expected) {
        // act
        String abbreviated = TypeNames.abbreviate(type, basePackage);

        // assert
        assertThat(abbreviated).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "com.other.Thing, com.acme.order, true, c.o.Thing", "com.other.Thing, com.acme.order, false, com.other.Thing",
            "com.acme.order.Thing, com.acme.order, true, c.a.o.Thing" })
    void abbreviatesForeignTypesOnRequest(String type, String basePackage, boolean foreign, String expected) {
        // act
        String abbreviated = TypeNames.abbreviate(type, basePackage, foreign);

        // assert
        assertThat(abbreviated).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "com.acme.Order, com.acme, Order, Order",
            "Order, '', Order, Order",
            "com.acme.Order$Line, com.acme, Order.Line, Line"
    })
    void splitsNames(String type, String packageName, String shortName, String simpleName) {
        // act
        String actualPackage = TypeNames.packageOf(type);
        String actualShort = TypeNames.shortName(type);
        String actualSimple = TypeNames.simpleName(type);

        // assert
        assertThat(actualPackage).isEqualTo(packageName);
        assertThat(actualShort).isEqualTo(shortName);
        assertThat(actualSimple).isEqualTo(simpleName);
    }
}
