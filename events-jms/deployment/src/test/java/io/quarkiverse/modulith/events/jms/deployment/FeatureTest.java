package io.quarkiverse.modulith.events.jms.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jms.deployment.fixtures.support.FakeConnectionFactory;
import io.quarkus.test.QuarkusExtensionTest;

public class FeatureTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addClasses(FakeConnectionFactory.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .setLogRecordPredicate(r -> Objects.nonNull(r.getMessage()) && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(r -> assertThat(Arrays.toString(r.getParameters())).contains("modulith-events-jms")));

    @Test
    void registersTheFeature() {
    }
}
