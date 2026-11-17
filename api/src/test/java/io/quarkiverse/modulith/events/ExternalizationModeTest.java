package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalizationModeTest {

    @Test
    void namesTheModeProperty() {
        // act
        String property = ExternalizationMode.PROPERTY;

        // assert
        assertThat(property).isEqualTo("quarkus.modulith.events.externalization.mode");
    }

    @Test
    void offersTheModuleListenerAndTheOutboxModes() {
        // act
        ExternalizationMode[] modes = ExternalizationMode.values();

        // assert
        assertThat(modes).containsExactly(ExternalizationMode.MODULE_LISTENER, ExternalizationMode.OUTBOX);
    }
}
