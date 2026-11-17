package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.fixtures.app.billing.CustomerCreated;
import io.quarkiverse.modulith.events.fixtures.app.billing.DoubleBilled;
import io.quarkiverse.modulith.events.fixtures.app.billing.InvoiceIssued;
import io.quarkiverse.modulith.events.fixtures.app.inventory.BothAnnotations;
import io.quarkiverse.modulith.events.fixtures.app.inventory.StockChanged;
import io.quarkiverse.modulith.events.fixtures.hidden.HiddenHolder;

class AnnotationTargetLookupTest {

    @Test
    void cachesTheTargetPerType() {
        // act
        Optional<ParsedRoutingTarget> first = AnnotationTargetLookup.targetOf(DoubleBilled.class);
        Optional<ParsedRoutingTarget> second = AnnotationTargetLookup.targetOf(DoubleBilled.class);

        // assert
        assertThat(first).contains(ParsedRoutingTarget.parse("billing")).isSameAs(second);
    }

    @ParameterizedTest
    @MethodSource("annotatedTypes")
    void findsEverySupportedAnnotationNearestFirst(Class<?> type, List<String> expected) {
        // act
        List<String> found = AnnotationTargetLookup.find(type).map(Annotation::annotationType).map(Class::getName)
                .toList();

        // assert
        assertThat(found).isEqualTo(expected);
    }

    static List<Object[]> annotatedTypes() {
        return List.of(
                new Object[] { BothAnnotations.class,
                        List.of(Externalized.class.getName(), AnnotationTargetLookup.JMOLECULES_EXTERNALIZED) },
                new Object[] { CustomerCreated.class, List.of(Externalized.class.getName()) },
                new Object[] { InvoiceIssued.class, List.of() });
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "value|stock", "target|''", "missing|''" })
    void readsAttributesOfForeignAnnotations(String attribute, String expected) {
        // arrange
        Annotation jMolecules = AnnotationTargetLookup.find(StockChanged.class).findFirst().orElseThrow();

        // act
        String value = AnnotationTargetLookup.attribute(jMolecules, attribute);

        // assert
        assertThat(value).isEqualTo(expected);
    }

    @Test
    void reportsUnreadableAttributes() {
        // arrange
        Annotation hidden = AnnotationLookup.findAll(HiddenHolder.class, annotation -> true).findFirst()
                .orElseThrow();

        // act & assert
        assertThatThrownBy(() -> AnnotationTargetLookup.attribute(hidden, "target"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read attribute target of io.quarkiverse.modulith.events.fixtures.hidden.Hidden")
                .hasCauseInstanceOf(IllegalAccessException.class);
    }

    @Test
    void requiresAnEvent() {
        // act & assert
        assertThatThrownBy(() -> AnnotationTargetLookup.hasExternalizedAnnotation(null))
                .isInstanceOf(NullPointerException.class).hasMessage("event");
    }
}
