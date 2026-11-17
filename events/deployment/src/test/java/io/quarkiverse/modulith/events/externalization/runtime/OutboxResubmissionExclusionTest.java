package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;

class OutboxResubmissionExclusionTest {

    @ParameterizedTest
    @CsvSource({
            "io.quarkiverse.modulith.events.externalization.runtime.OutboxEventExternalizer.externalize(java.lang.Object),true",
            "io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener.externalize(java.lang.Object),false",
            "com.acme.Listener.on(com.acme.Event),false" })
    void keepsTheRegistryAwayFromOutboxPublications(String listener, boolean excluded) {
        // act
        boolean excludes = new OutboxResubmissionExclusion().excludes(PublicationTargetIdentifier.of(listener));

        // assert
        assertThat(excludes).isEqualTo(excluded);
    }
}
