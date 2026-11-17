package io.quarkiverse.modulith.moments.runtime;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.moments.support.MomentsProperties;

/**
 * What {@link MomentsProducer} and {@link TimeMachineProducer} share.
 */
final class MomentsBeans {

    private static final Logger LOG = Logger.getLogger(MomentsBeans.class);

    private MomentsBeans() {
    }

    /**
     * The application's {@link Clock} bean if there is one, {@link Clock#systemUTC()} otherwise. Resolved once, when
     * the {@code Moments} bean is created.
     */
    static Clock clock(Instance<Clock> clocks) {
        return clocks.isUnsatisfied() ? Clock.systemUTC() : clocks.get();
    }

    static MomentsProperties properties(MomentsConfig config, boolean enableTimeMachine) {
        config.locale().ifPresent(MomentsBeans::warnIfUnavailable);
        return MomentsProperties.of(config.granularity(), config.zoneId(), config.locale().orElse(Locale.getDefault()),
                config.quarterStartMonth(), enableTimeMachine);
    }

    /**
     * A native executable only has the data of the locales of {@code quarkus.locales}. For any other locale
     * {@code WeekFields} may silently use the week rule of another one, so weeks would end on the wrong day.
     */
    private static void warnIfUnavailable(Locale locale) {
        if (!List.of(Locale.getAvailableLocales()).contains(locale)) {
            LOG.warnf("The locale %s of quarkus.modulith.moments.locale is not available, so WeekHasPassed may follow"
                    + " the week rule of another locale. In a native executable, add it to quarkus.locales",
                    locale.toLanguageTag());
        }
    }
}
