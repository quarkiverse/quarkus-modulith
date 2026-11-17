package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

public class ModelLoggingTest {

    static final String PROCESSOR = "io.quarkiverse.modulith.deployment.ModelProcessor";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.log.category.\"" + PROCESSOR + "\".min-level", "DEBUG")
            .overrideConfigKey("quarkus.log.category.\"" + PROCESSOR + "\".level", "DEBUG")
            .setLogRecordPredicate(it -> it.getLoggerName().equals(PROCESSOR))
            .assertLogRecords(records -> assertThat(records).filteredOn(it -> it.getLevel().equals(Level.DEBUG))
                    .extracting(ModelLoggingTest::message).singleElement().asString()
                    .startsWith("Detected 5 application modules in " + ShopApplication.class.getPackageName() + ": [")
                    .contains("inventory listening to [" + ShopApplication.class.getPackageName()
                            + ".order.OrderCompleted]"));

    @Test
    void summarizesTheModelAtDebugLevel() {
    }

    private static String message(LogRecord record) {
        return String.format(record.getMessage(), record.getParameters());
    }
}
