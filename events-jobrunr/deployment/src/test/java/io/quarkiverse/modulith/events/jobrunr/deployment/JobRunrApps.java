package io.quarkiverse.modulith.events.jobrunr.deployment;

import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.Jobs;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Builds the test applications: the shop module, an in-memory H2 data source and a JobRunr background job server that
 * polls every five seconds, the shortest interval it supports, and retries after one second.
 */
final class JobRunrApps {

    private JobRunrApps() {
    }

    static QuarkusExtensionTest app(String mode, Class<?>... classes) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                        .addClasses(Jobs.class, JobRunrApps.class).addClasses(classes))
                .overrideConfigKey("quarkus.modulith.base-package",
                        "io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop")
                .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
                .overrideConfigKey("quarkus.modulith.events.externalization.mode", mode)
                .overrideConfigKey("quarkus.datasource.db-kind", "h2")
                .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:jobs;DB_CLOSE_DELAY=-1")
                .overrideConfigKey("quarkus.jobrunr.background-job-server.enabled", "true")
                .overrideConfigKey("quarkus.jobrunr.background-job-server.poll-interval-in-seconds", "5")
                .overrideConfigKey("quarkus.jobrunr.jobs.retry-back-off-time-seed", "1")
                .overrideConfigKey("quarkus.jobrunr.dashboard.enabled", "false");
    }
}
