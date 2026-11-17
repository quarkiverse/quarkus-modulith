package io.quarkiverse.modulith.events.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FeatureTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.devservices.image-name", "mongo:7.0.48")
            .setLogRecordPredicate(r -> Objects.nonNull(r.getMessage()) && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(r -> assertThat(Arrays.toString(r.getParameters())).contains("modulith-events-mongodb")));

    @Test
    void registersTheFeature() {
    }
}
