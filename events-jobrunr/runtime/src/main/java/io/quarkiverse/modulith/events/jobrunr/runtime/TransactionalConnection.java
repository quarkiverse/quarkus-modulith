package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Objects;
import java.util.Set;

import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * A connection enlisted in a JTA transaction, as JobRunr sees it: {@code commit()} and {@code setAutoCommit} do nothing,
 * {@code rollback()} marks the transaction for rollback, everything else goes to the connection.
 */
final class TransactionalConnection implements InvocationHandler {

    private static final Set<String> LEFT_TO_THE_TRANSACTION = Set.of("commit", "setAutoCommit");

    private final Connection connection;
    private final TransactionSynchronizationRegistry transactions;

    TransactionalConnection(Connection connection, TransactionSynchronizationRegistry transactions) {
        this.connection = connection;
        this.transactions = transactions;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (LEFT_TO_THE_TRANSACTION.contains(method.getName())) {
            return null;
        }
        if ("rollback".equals(method.getName()) && Objects.isNull(args)) {
            transactions.setRollbackOnly();
            return null;
        }
        try {
            return method.invoke(connection, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
