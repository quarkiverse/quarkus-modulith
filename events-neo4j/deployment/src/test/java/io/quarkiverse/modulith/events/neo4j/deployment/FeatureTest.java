package io.quarkiverse.modulith.events.neo4j.deployment;

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
            .overrideConfigKey("quarkus.neo4j.devservices.image-name", "neo4j:5.26.33")
            .setLogRecordPredicate(r -> Objects.nonNull(r.getMessage()) && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(r -> assertThat(Arrays.toString(r.getParameters())).contains("modulith-events-neo4j")));

    @Test
    void registersTheFeature() {
    }
}
