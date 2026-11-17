package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.named.DefaultNamed;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.named.KeyGenerator;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.named.Producers;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.named.Thing;

class NamedBeansTest {

    private static final String NAMED = "io.quarkiverse.modulith.events.deployment.externalization.fixtures.named.";

    @Test
    void findsNamedClassesAndProducers() throws IOException {
        // arrange
        Index index = Index.of(KeyGenerator.class, DefaultNamed.class, Producers.class, Thing.class);

        // act
        Map<String, Type> beans = NamedBeans.scan(index);

        // assert
        assertThat(beans).hasSize(7);
        assertThat(beans.keySet()).containsExactly("defaultField", "defaultNamed", "get", "keys", "produced",
                "producedField", "property");
        assertThat(beans.get("keys").name()).hasToString(NAMED + "KeyGenerator");
        assertThat(beans.get("defaultNamed").name()).hasToString(NAMED + "DefaultNamed");
        assertThat(beans.values()).filteredOn(type -> type.name().toString().equals(NAMED + "Thing")).hasSize(5);
    }

    @ParameterizedTest
    @CsvSource({ "getProperty,property", "getURL,uRL", "get,get", "produce,produce", "getter,ter" })
    void namesProducerMethodsLikeCdi(String method, String name) {
        // act
        String property = NamedBeans.propertyName(method);

        // assert
        assertThat(property).isEqualTo(name);
    }
}
