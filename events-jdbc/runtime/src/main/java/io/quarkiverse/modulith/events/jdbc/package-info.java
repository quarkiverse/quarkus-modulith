/**
 * Event publication registry stored with plain JDBC on an Agroal datasource, in the tables of Spring Modulith's JDBC
 * store (`EVENT_PUBLICATION` and `EVENT_PUBLICATION_ARCHIVE`, structure version 1 or 2).
 * <p>
 * The table definitions for every supported database are on the class path under {@code db/modulith/events/jdbc}, for
 * applications that create the tables with Flyway or Liquibase.
 */
package io.quarkiverse.modulith.events.jdbc;
