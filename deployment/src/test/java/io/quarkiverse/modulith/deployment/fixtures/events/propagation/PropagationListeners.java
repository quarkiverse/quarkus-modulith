package io.quarkiverse.modulith.deployment.fixtures.events.propagation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.transaction.Status;
import jakarta.transaction.Transactional.TxType;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Records whether each listener ran in a transaction.
 */
public class PropagationListeners {

    public static final Map<String, Boolean> IN_TRANSACTION = new ConcurrentHashMap<>();

    @ApplicationModuleListener(async = false, propagation = TxType.REQUIRED)
    void required(Recorded event) {
        IN_TRANSACTION.put("REQUIRED", inTransaction());
    }

    @ApplicationModuleListener(async = false)
    void requiresNew(Recorded event) {
        IN_TRANSACTION.put("REQUIRES_NEW", inTransaction());
    }

    @ApplicationModuleListener(async = false, propagation = TxType.SUPPORTS)
    void supports(Recorded event) {
        IN_TRANSACTION.put("SUPPORTS", inTransaction());
    }

    @ApplicationModuleListener(async = false, propagation = TxType.NOT_SUPPORTED, readOnlyTransaction = true)
    void notSupported(Recorded event) {
        IN_TRANSACTION.put("NOT_SUPPORTED", inTransaction());
    }

    @ApplicationModuleListener(async = false, propagation = TxType.NEVER)
    void never(Recorded event) {
        IN_TRANSACTION.put("NEVER", inTransaction());
    }

    private static boolean inTransaction() {
        return QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION;
    }
}
