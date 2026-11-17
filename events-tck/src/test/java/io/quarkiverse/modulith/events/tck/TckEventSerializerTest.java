package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TckEventSerializerTest {

    private final TckEventSerializer serializer = new TckEventSerializer();

    @Test
    void writesCompactJsonAndReadsItBack() {
        // act
        Object serialized = serializer.serialize(new TckEvent("e-1"));
        TckEvent read = serializer.deserialize(serialized, TckEvent.class);

        // assert
        assertThat(serialized).isEqualTo("{\"id\":\"e-1\"}");
        assertThat(read).isEqualTo(new TckEvent("e-1"));
    }
}
