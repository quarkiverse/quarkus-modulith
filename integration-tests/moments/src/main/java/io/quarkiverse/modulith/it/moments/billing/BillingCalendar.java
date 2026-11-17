package io.quarkiverse.modulith.it.moments.billing;

import jakarta.inject.Inject;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.MonthHasPassed;
import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.WeekHasPassed;
import io.quarkiverse.modulith.moments.YearHasPassed;

/**
 * Reacts to the passage of time as a billing module would: hourly usage, daily and weekly reports, monthly invoices,
 * quarterly and yearly closing.
 */
public class BillingCalendar {

    @Inject
    ReceivedMoments received;

    @ApplicationModuleListener
    void on(HourHasPassed event) {
        received.add(event);
    }

    @ApplicationModuleListener
    void on(DayHasPassed event) {
        received.add(event);
    }

    @ApplicationModuleListener
    void on(WeekHasPassed event) {
        received.add(event);
    }

    @ApplicationModuleListener
    void on(MonthHasPassed event) {
        received.add(event);
    }

    @ApplicationModuleListener
    void on(QuarterHasPassed event) {
        received.add(event);
    }

    @ApplicationModuleListener
    void on(YearHasPassed event) {
        received.add(event);
    }
}
