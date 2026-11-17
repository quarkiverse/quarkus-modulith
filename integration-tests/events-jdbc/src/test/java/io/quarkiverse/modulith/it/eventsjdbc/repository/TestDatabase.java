package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;

/**
 * The databases the JDBC repository is tested on, on the official images of each vendor with pinned tags.
 */
public enum TestDatabase {

    H2(DatabaseType.H2, false),
    HSQLDB(DatabaseType.HSQLDB, false),
    POSTGRESQL(DatabaseType.POSTGRESQL, false),
    MYSQL(DatabaseType.MYSQL, false),
    MARIADB(DatabaseType.MARIADB, false),
    SQLSERVER(DatabaseType.SQLSERVER, true),
    ORACLE(DatabaseType.ORACLE, true);

    static final String SQLSERVER_IMAGE = "mcr.microsoft.com/mssql/server:2022-CU28-ubuntu-22.04";
    static final String ORACLE_IMAGE = "container-registry.oracle.com/database/free:23.26.3.0-lite";
    private static final Duration HEAVY_STARTUP_TIMEOUT = Duration.ofMinutes(5);

    private final DatabaseType type;
    private final boolean heavy;

    TestDatabase(DatabaseType type, boolean heavy) {
        this.type = type;
        this.heavy = heavy;
    }

    public DatabaseType type() {
        return type;
    }

    /**
     * @return whether the container needs so much memory that it never runs next to another heavy one
     */
    public boolean heavy() {
        return heavy;
    }

    /**
     * @return a new, not started container, empty for the in-process databases
     */
    Optional<JdbcDatabaseContainer<?>> container() {
        return switch (this) {
            case POSTGRESQL -> Optional.of(new PostgreSQLContainer("postgres:18.7"));
            case MYSQL -> Optional.of(new MySQLContainer("mysql:8.4.13"));
            case MARIADB -> Optional.of(new MariaDBContainer("mariadb:11.8.9"));
            case SQLSERVER -> Optional.of(sqlServer());
            case ORACLE -> Optional.of(oracle());
            default -> Optional.empty();
        };
    }

    /**
     * SQL Server accepts TCP connections before it accepts logins, so polling it with JDBC right away fails with
     * pre-login errors until it is ready. The container first waits for the log line that says it accepts client
     * connections, then checks with a query as usual.
     */
    private static MSSQLServerContainer sqlServer() {
        return new MSSQLServerContainer(SQLSERVER_IMAGE) {

            @Override
            protected void waitUntilContainerStarted() {
                Wait.forLogMessage(".*SQL Server is now ready for client connections.*", 1)
                        .withStartupTimeout(HEAVY_STARTUP_TIMEOUT)
                        .waitUntilReady(this);
                super.waitUntilContainerStarted();
            }
        }.acceptLicense();
    }

    /**
     * Oracle's own image has no switch for an application user, so a startup script creates the user the container
     * class connects with. Startup scripts run after the image reports the database ready, so the wait is for the
     * script's own last line.
     */
    private static OracleContainer oracle() {
        return new OracleContainer(DockerImageName.parse(ORACLE_IMAGE).asCompatibleSubstituteFor("gvenzl/oracle-free"))
                .withEnv("ORACLE_PWD", "modulith-tests")
                .withCopyFileToContainer(MountableFile.forClasspathResource("oracle/create-test-user.sql"),
                        "/opt/oracle/scripts/startup/01-create-test-user.sql")
                .waitingFor(Wait.forLogMessage(".*MODULITH TEST USER READY.*", 1)
                        .withStartupTimeout(HEAVY_STARTUP_TIMEOUT));
    }

    /**
     * @return the databases to test, all of them unless the {@code modulith.test.databases} system property names
     *         some, for example {@code -Dmodulith.test.databases=ORACLE}
     */
    static Stream<TestDatabase> selected(Stream<TestDatabase> databases) {
        String names = System.getProperty("modulith.test.databases", "");
        return databases.filter(database -> names.isBlank() || Arrays.asList(names.split(",")).contains(database.name()));
    }

    /**
     * @return a timestamp literal for the given {@code yyyy-MM-dd HH:mm:ss} value
     */
    String timestamp(String value) {
        return this == SQLSERVER ? "'" + value + "'" : "TIMESTAMP '" + value + "'";
    }

    /**
     * @return the JDBC driver class of an in-process database
     */
    String inProcessDriver() {
        return this == H2 ? "org.h2.Driver" : "org.hsqldb.jdbc.JDBCDriver";
    }

    /**
     * @return the JDBC URL of an in-process database
     */
    String inProcessUrl() {
        return this == H2 ? "jdbc:h2:mem:events;DB_CLOSE_DELAY=-1" : "jdbc:hsqldb:mem:events";
    }
}
