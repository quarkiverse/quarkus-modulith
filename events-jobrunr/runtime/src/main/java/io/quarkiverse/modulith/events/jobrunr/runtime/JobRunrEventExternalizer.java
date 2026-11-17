package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.function.BooleanSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Status;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * The outbox mode through JobRunr, Spring Modulith's {@code JobRunrEventExternalizer}: every event the configuration
 * supports becomes JobRunr jobs, saved in the publishing transaction on the shared data source, so that a rolled back
 * transaction enqueues nothing. JobRunr's background job server runs the jobs after the commit and retries failed ones.
 * <p>
 * Without a transaction the jobs are saved right away, off the I/O thread when the event was published on one.
 */
@Singleton
public class JobRunrEventExternalizer implements ExternalizationStrategy {

    private final EventExternalizationConfiguration configuration;
    private final JobRunrExternalizationJobs jobs;
    private final ListenerExecutor executor;
    private final BooleanSupplier transactionActive;
    private final BooleanSupplier blockingAllowed;

    @Inject
    public JobRunrEventExternalizer(EventExternalizationConfiguration configuration, JobRunrExternalizationJobs jobs,
            ListenerExecutor executor) {
        this(configuration, jobs, executor, JobRunrEventExternalizer::inTransaction,
                BlockingOperationControl::isBlockingAllowed);
    }

    JobRunrEventExternalizer(EventExternalizationConfiguration configuration, JobRunrExternalizationJobs jobs,
            ListenerExecutor executor, BooleanSupplier transactionActive, BooleanSupplier blockingAllowed) {
        this.configuration = configuration;
        this.jobs = jobs;
        this.executor = executor;
        this.transactionActive = transactionActive;
        this.blockingAllowed = blockingAllowed;
    }

    /**
     * @return whether the current thread has a transaction, in any state
     */
    static boolean inTransaction() {
        return QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION;
    }

    @Override
    public void onEvent(Object event) {
        if (!configuration.supports(event)) {
            return;
        }
        if (transactionActive.getAsBoolean() || blockingAllowed.getAsBoolean()) {
            jobs.enqueue(event);
        } else {
            executor.execute(() -> jobs.enqueue(event));
        }
    }
}
