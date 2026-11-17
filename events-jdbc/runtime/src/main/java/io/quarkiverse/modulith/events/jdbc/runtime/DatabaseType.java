package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The databases Spring Modulith ships event publication schemas for, with the differences the repository needs: how
 * identifiers are stored, whether a schema name can be set, and how a result is limited.
 */
public enum DatabaseType {

    H2("h2", "H2", false, true, " LIMIT %d"),
    HSQLDB("hsqldb", "HSQL Database Engine", false, true, " LIMIT %d"),
    POSTGRESQL("postgresql", "PostgreSQL", false, true, " LIMIT %d"),
    MYSQL("mysql", "MySQL", true, false, " LIMIT %d"),
    MARIADB("mariadb", "MariaDB", true, false, " LIMIT %d"),
    SQLSERVER("sqlserver", "Microsoft SQL Server", true, false, " OFFSET 0 ROWS FETCH NEXT %d ROWS ONLY"),
    ORACLE("oracle", "Oracle", true, false, " FETCH FIRST %d ROWS ONLY");

    static final String SCHEMA_ROOT = "db/modulith/events/jdbc";

    private final String value;
    private final String productName;
    private final boolean uuidAsString;
    private final boolean schemaSupported;
    private final String limitClause;

    DatabaseType(String value, String productName, boolean uuidAsString, boolean schemaSupported, String limitClause) {
        this.value = value;
        this.productName = productName;
        this.uuidAsString = uuidAsString;
        this.schemaSupported = schemaSupported;
        this.limitClause = limitClause;
    }

    /**
     * @param productName the database product name the JDBC driver reports
     * @return the matching database type
     * @throws IllegalStateException if the database is not supported
     */
    public static DatabaseType fromProductName(String productName) {
        return Arrays.stream(values())
                .filter(type -> type.productName.equalsIgnoreCase(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unsupported database for the JDBC event publication registry: "
                        + productName + ". Supported: " + Arrays.stream(values()).map(type -> type.productName)
                                .collect(Collectors.joining(", "))));
    }

    /**
     * @return the name used in the schema file names, for example {@code postgresql}
     */
    public String value() {
        return value;
    }

    public boolean isSchemaSupported() {
        return schemaSupported;
    }

    Object uuidToDatabase(UUID identifier) {
        return uuidAsString ? identifier.toString() : identifier;
    }

    static UUID databaseToUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    String limitClause(int limit) {
        return limitClause.formatted(limit);
    }

    /**
     * @param legacy whether the schema of the legacy (version 1) structure is wanted
     * @return the class path resource with the table definition
     */
    public String schemaResource(boolean legacy) {
        return schemaBase(legacy) + ".sql";
    }

    /**
     * @param legacy whether the schema of the legacy (version 1) structure is wanted
     * @return the class path resource with the archive table definition
     */
    public String archiveSchemaResource(boolean legacy) {
        return schemaBase(legacy) + "-archive.sql";
    }

    private String schemaBase(boolean legacy) {
        return SCHEMA_ROOT + (legacy ? "/v1" : "/v2") + "/schema-" + value;
    }
}
