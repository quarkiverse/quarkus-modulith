package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The build registers the feature. The assertion runs on the startup log, in {@code assertLogRecords}.
 */
public class FeatureTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .setLogRecordPredicate(r -> Objects.nonNull(r.getMessage()) && r.getMessage().contains("Installed features"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(r -> assertThat(Arrays.toString(r.getParameters())).contains("modulith-moments")));

    @Test
    void registersTheFeature() {
    }
}
