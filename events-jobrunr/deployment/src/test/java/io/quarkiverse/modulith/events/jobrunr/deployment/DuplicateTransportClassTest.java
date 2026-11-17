package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.DuplicateTransports;
import io.quarkus.test.QuarkusExtensionTest;

class DuplicateTransportClassTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", DuplicateTransports.class)
            .assertException(failure -> assertThat(failure).hasStackTraceContaining(DuplicateTransports.class.getName()
                    + " declares 2 JobRunrExternalizationTransport beans, but JobRunr jobs name a transport by its bean"
                    + " class: declare each transport in a class of its own"));

    @Test
    void failsTheBuild() {
    }
}
