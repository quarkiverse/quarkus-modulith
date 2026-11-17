package io.quarkiverse.modulith.events.jobrunr.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.logging.Logger;

import org.h2.jdbc.JdbcConnection;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionalDataSourceTest {

    private final JdbcDataSource h2 = new JdbcDataSource();
    private final FakeTransactions transactions = new FakeTransactions();
    private final TransactionalDataSource dataSource = new TransactionalDataSource(h2, transactions);
    private Connection keeper;

    @BeforeEach
    void createTable() throws SQLException {
        h2.setURL("jdbc:h2:mem:transactional");
        h2.setUser("sa");
        h2.setPassword("");
        keeper = h2.getConnection();
        try (Statement statement = keeper.createStatement()) {
            statement.execute("create table jobs (id int)");
        }
    }

    @AfterEach
    void dropTable() throws SQLException {
        try (Statement statement = keeper.createStatement()) {
            statement.execute("drop table jobs");
        }
        keeper.close();
    }

    private static int count(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("select count(*) from jobs")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static void insert(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("insert into jobs values (1)");
        }
    }

    @Test
    void handsOutTheDataSourcesConnectionsOutsideATransaction() throws SQLException {
        // act
        try (Connection connection = dataSource.getConnection()) {
            // assert
            assertThat(connection).isInstanceOf(JdbcConnection.class);
        }
    }

    @Test
    void wrapsTheConnectionsTakenInATransaction() throws SQLException {
        // arrange
        transactions.begin();

        // act
        try (Connection connection = dataSource.getConnection("sa", "")) {
            // assert
            assertThat(connection).isNotInstanceOf(JdbcConnection.class);
            assertThat(connection.unwrap(JdbcConnection.class)).isInstanceOf(JdbcConnection.class);
        }
    }

    @Test
    void leavesTheCommitToTheTransaction() throws SQLException {
        // arrange
        transactions.begin();
        try (Connection connection = dataSource.getConnection()) {
            Connection raw = connection.unwrap(JdbcConnection.class);
            raw.setAutoCommit(false);
            insert(connection);

            // act
            connection.commit();

            // assert
            assertThat(count(keeper)).isZero();
            assertThat(transactions.getRollbackOnly()).isFalse();
        }
    }

    @Test
    void ignoresAutoCommitChanges() throws SQLException {
        // arrange
        transactions.begin();
        try (Connection connection = dataSource.getConnection()) {
            Connection raw = connection.unwrap(JdbcConnection.class);
            raw.setAutoCommit(false);

            // act
            connection.setAutoCommit(true);

            // assert
            assertThat(raw.getAutoCommit()).isFalse();
        }
    }

    @Test
    void turnsARollbackIntoARollbackOfTheTransaction() throws SQLException {
        // arrange
        transactions.begin();
        try (Connection connection = dataSource.getConnection()) {
            Connection raw = connection.unwrap(JdbcConnection.class);
            raw.setAutoCommit(false);
            insert(connection);

            // act
            connection.rollback();

            // assert
            assertThat(transactions.getRollbackOnly()).isTrue();
            assertThat(count(raw)).isEqualTo(1);
        }
    }

    @Test
    void rollsBackToASavepointOnTheConnection() throws SQLException {
        // arrange
        transactions.begin();
        try (Connection connection = dataSource.getConnection()) {
            connection.unwrap(JdbcConnection.class).setAutoCommit(false);
            Savepoint savepoint = connection.setSavepoint();
            insert(connection);

            // act
            connection.rollback(savepoint);

            // assert
            assertThat(count(connection)).isZero();
            assertThat(transactions.getRollbackOnly()).isFalse();
        }
    }

    @Test
    void throwsTheFailuresOfTheConnection() throws SQLException {
        // arrange
        transactions.begin();
        try (Connection connection = dataSource.getConnection()) {
            // act & assert
            assertThatThrownBy(() -> connection.prepareStatement("not sql"))
                    .isInstanceOf(JdbcSQLSyntaxErrorException.class);
        }
    }

    @Test
    void delegatesTheSettings() throws SQLException {
        // arrange
        PrintWriter writer = new PrintWriter(new StringWriter());

        // act
        dataSource.setLogWriter(writer);
        dataSource.setLoginTimeout(7);

        // assert
        assertThat(dataSource.getLogWriter()).isSameAs(writer);
        assertThat(h2.getLogWriter()).isSameAs(writer);
        assertThat(dataSource.getLoginTimeout()).isEqualTo(7);
        assertThat(h2.getLoginTimeout()).isEqualTo(7);
    }

    @Test
    void delegatesTheParentLogger() throws SQLFeatureNotSupportedException {
        // act
        Logger logger = dataSource.getParentLogger();

        // assert
        assertThat(logger).isSameAs(h2.getParentLogger());
    }

    @Test
    void unwrapsToItselfOrTheDataSource() throws SQLException {
        // act
        TransactionalDataSource itself = dataSource.unwrap(TransactionalDataSource.class);
        JdbcDataSource delegate = dataSource.unwrap(JdbcDataSource.class);

        // assert
        assertThat(itself).isSameAs(dataSource);
        assertThat(delegate).isSameAs(h2);
        assertThat(dataSource.isWrapperFor(TransactionalDataSource.class)).isTrue();
        assertThat(dataSource.isWrapperFor(JdbcDataSource.class)).isTrue();
        assertThat(dataSource.isWrapperFor(String.class)).isFalse();
    }
}
