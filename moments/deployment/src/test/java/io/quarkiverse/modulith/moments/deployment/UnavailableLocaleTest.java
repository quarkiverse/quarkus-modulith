package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.support.Moments;
import io.quarkus.test.QuarkusExtensionTest;

class UnavailableLocaleTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.scheduler.enabled", "false")
            .overrideConfigKey("quarkus.modulith.moments.locale", "xx-YY")
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkiverse.modulith.moments")
                    && record.getLevel().equals(Level.WARNING))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(record -> record.getMessage() + " " + Arrays.toString(record.getParameters()))
                    .containsExactly("The locale %s of quarkus.modulith.moments.locale is not available, so"
                            + " WeekHasPassed may follow the week rule of another locale. In a native executable, add"
                            + " it to quarkus.locales [xx-YY]"));

    @Inject
    Moments moments;

    @Test
    void startsWithTheLocale() {
        // act
        Class<?> type = moments.getClass();

        // assert
        assertThat(type).isEqualTo(Moments.class);
    }
}
