package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.data.TypeData;
import io.quarkiverse.modulith.docs.fixtures.config.ConfigApplication;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * The plain-test documenter lists the configuration properties the build lists: the canvas row "Properties".
 */
class ConfigPropertiesTest {

    static final String BILLING = "io.quarkiverse.modulith.docs.fixtures.config.billing.";

    @Retention(RetentionPolicy.RUNTIME)
    @interface Marker {
    }

    private static ModuleProperty property(String name, String type, String source, String defaultValue) {
        return new ModuleProperty(name, type, BILLING + source, Optional.empty(), Optional.ofNullable(defaultValue));
    }

    @Test
    void listsThePropertiesOfTheModuleTypes() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(ConfigApplication.class, ClassLocations.TESTS);

        // act
        DocumentationModel model = DocumentationModel.of(modules);

        // assert
        assertThat(model.details(modules.getModuleByName("billing").orElseThrow()).properties()).containsExactly(
                property("billing.copies", "java.util.Optional<java.util.List<java.lang.String>>", "BillingConfig", null),
                property("billing.currency", "java.lang.String", "Defaults", "EUR"),
                property("billing.fallback.attempts", "int", "Retry", "3"),
                property("billing.footer", "java.util.Optional<java.lang.String>", "BillingConfig", null),
                property("billing.iban", "java.lang.String", "Bank", null),
                property("billing.label", "java.lang.CharSequence", "BillingConfig", null),
                property("billing.max-lines", "int", "BillingConfig", null),
                property("billing.recipients", "java.util.List<java.lang.String>", "BillingConfig", null),
                property("billing.retries", "int", "InvoiceSender", "2"),
                property("billing.retry.attempts", "int", "Retry", "3"),
                property("billing.sender", "java.lang.String", "InvoiceSender", null),
                property("billing.tax-rate", "java.math.BigDecimal", "BillingConfig", "0.1"),
                property("dry-run", "boolean", "Flags", null),
                property(BILLING + "InvoiceSender.timeout", "long", "InvoiceSender", null));
    }

    @Test
    void listsNoPropertiesOfATypeThatCannotBeLoaded() {
        // arrange
        JavaType missing = JavaType.of(new TypeData("com.acme.Missing", 1, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));

        // act
        List<ModuleProperty> properties = new ConfigProperties(DocumentationSource.none()).of(missing);

        // assert
        assertThat(properties).isEmpty();
    }

    @Test
    void readsNoValueOfAnAttributeTheAnnotationLacks() {
        // arrange
        Annotation marker = new Marker() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Marker.class;
            }
        };

        // act
        Optional<String> value = ConfigProperties.value(marker, "value");

        // assert
        assertThat(value).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "maxSize, max-size", "region, region", "URLPath, url-path", "httpURL, http-url", "a1B, a1-b" })
    void derivesKebabCaseNames(String method, String property) {
        // act
        String kebab = ConfigProperties.kebab(method);

        // assert
        assertThat(kebab).isEqualTo(property);
    }
}
