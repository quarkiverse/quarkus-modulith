package io.quarkiverse.modulith.events.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.deployment.fixtures.Shipped;
import io.quarkiverse.modulith.events.deployment.fixtures.Unwritable;
import io.quarkus.test.QuarkusExtensionTest;

public class JacksonEventSerializerTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Shipped.class, Unwritable.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    EventSerializer serializer;

    @Test
    void roundTripsEvents() {
        // arrange
        Shipped event = new Shipped("o-1", 2);

        // act
        Object json = serializer.serialize(event);
        Shipped read = serializer.deserialize(json, Shipped.class);

        // assert
        assertThat(json).isEqualTo("{\"orderId\":\"o-1\",\"parcels\":2}");
        assertThat(read).isEqualTo(event);
    }

    @Test
    void reportsEventsItCannotWrite() {
        // arrange
        Unwritable event = new Unwritable();

        // act & assert
        assertThatThrownBy(() -> serializer.serialize(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize event " + Unwritable.class.getName());
    }

    @Test
    void reportsEventsItCannotRead() {
        // act & assert
        assertThatThrownBy(() -> serializer.deserialize("not json", Shipped.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read an event of type " + Shipped.class.getName());
    }
}
