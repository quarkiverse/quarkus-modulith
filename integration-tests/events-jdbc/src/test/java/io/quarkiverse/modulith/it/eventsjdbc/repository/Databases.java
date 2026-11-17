package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.sql.SQLException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;

/**
 * Starts each test database once per test JVM and keeps it for the following tests. At most one heavy database
 * (SQL Server, Oracle) runs at a time: starting one stops the other.
 */
public final class Databases {

    private static final Map<TestDatabase, AgroalDataSource> DATA_SOURCES = new EnumMap<>(TestDatabase.class);
    private static final Map<TestDatabase, JdbcDatabaseContainer<?>> CONTAINERS = new EnumMap<>(TestDatabase.class);

    private Databases() {
    }

    /**
     * @return a pooled data source of the database, started on first use
     */
    public static synchronized DataSource dataSource(TestDatabase database) {
        AgroalDataSource running = DATA_SOURCES.get(database);
        if (Objects.nonNull(running)) {
            return running;
        }
        if (database.heavy()) {
            CONTAINERS.keySet().stream().filter(TestDatabase::heavy).toList().forEach(Databases::stop);
        }
        AgroalDataSource started = start(database);
        DATA_SOURCES.put(database, started);
        return started;
    }

    /**
     * @return a data source whose connections always fail
     */
    public static DataSource unreachable() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:missing;IFEXISTS=TRUE");
        return dataSource;
    }

    private static AgroalDataSource start(TestDatabase database) {
        return database.container().map(container -> {
            container.start();
            CONTAINERS.put(database, container);
            return create(container.getDriverClassName(), container.getJdbcUrl(), container.getUsername(),
                    container.getPassword());
        }).orElseGet(() -> create(database.inProcessDriver(), database.inProcessUrl(), "sa", ""));
    }

    private static void stop(TestDatabase database) {
        DATA_SOURCES.remove(database).close();
        CONTAINERS.remove(database).stop();
    }

    /**
     * The driver class is named so Agroal does not go through {@code DriverManager}, which no longer sees the drivers
     * of this class loader once a Quarkus test of the same JVM registered them from its own.
     */
    private static AgroalDataSource create(String driver, String url, String user, String password) {
        try {
            return AgroalDataSource.from(new AgroalDataSourceConfigurationSupplier()
                    .connectionPoolConfiguration(pool -> pool.maxSize(4).acquisitionTimeout(Duration.ofSeconds(5))
                            .connectionFactoryConfiguration(factory -> factory.connectionProviderClassName(driver)
                                    .jdbcUrl(url)
                                    .principal(new NamePrincipal(user))
                                    .credential(new SimplePassword(password)))));
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create a data source for " + url, e);
        }
    }
}
