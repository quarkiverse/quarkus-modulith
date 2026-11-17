package io.quarkiverse.modulith.events.jdbc.deployment;

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
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:feature")
            .setLogRecordPredicate(r -> Objects.nonNull(r.getMessage()) && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(r -> assertThat(Arrays.toString(r.getParameters())).contains("modulith-events-jdbc")));

    @Test
    void registersTheFeature() {
    }
}
