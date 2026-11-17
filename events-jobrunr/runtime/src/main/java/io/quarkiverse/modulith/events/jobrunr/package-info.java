/**
 * Externalizes module events through JobRunr jobs enqueued in the business transaction, like Spring Modulith's
 * {@code spring-modulith-events-jobrunr}. Applications implement {@link JobRunrExternalizationTransport} to send the
 * events of the jobs.
 */
package io.quarkiverse.modulith.events.jobrunr;
