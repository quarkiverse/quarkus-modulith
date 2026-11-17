package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * The data source JobRunr's SQL storage provider uses, so that jobs saved in a JTA transaction are part of it, like
 * Spring Modulith's {@code CommitSuppressingDataSourceProxy}. Agroal enlists a connection taken in a transaction and
 * refuses {@code commit}, {@code rollback} and {@code setAutoCommit(true)} on it, which JobRunr calls around each write.
 * Connections taken in a transaction therefore leave the commit to the transaction, and a rollback marks the transaction
 * for rollback. Connections taken outside a transaction are the data source's own.
 */
public class TransactionalDataSource implements DataSource {

    private final DataSource delegate;
    private final TransactionSynchronizationRegistry transactions;

    public TransactionalDataSource(DataSource delegate, TransactionSynchronizationRegistry transactions) {
        this.delegate = delegate;
        this.transactions = transactions;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return participating(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return participating(delegate.getConnection(username, password));
    }

    private Connection participating(Connection connection) {
        if (transactions.getTransactionStatus() == Status.STATUS_NO_TRANSACTION) {
            return connection;
        }
        return (Connection) Proxy.newProxyInstance(TransactionalDataSource.class.getClassLoader(),
                new Class<?>[] { Connection.class }, new TransactionalConnection(connection, transactions));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> type) throws SQLException {
        return type.isInstance(this) ? type.cast(this) : delegate.unwrap(type);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) throws SQLException {
        return type.isInstance(this) || delegate.isWrapperFor(type);
    }
}
