package io.quarkiverse.modulith.flyway.deployment;

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import db.migration.reports.V1__Reports;
import db.migration.shipping.V2__ShippingJava;
import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.audit.AuditLog;
import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.billing.Invoices;
import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.legacy.LegacyImport;
import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.reports.Reports;
import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.shipping.Shipments;

/**
 * The shop fixture: billing depends on shipping, audit has no migrations, legacy is skipped by a filter, reports
 * has a Java migration only.
 */
public final class ShopApplication {

    public static final String BASE_PACKAGE = "io.quarkiverse.modulith.flyway.deployment.fixtures.shop";

    private ShopApplication() {
    }

    public static Consumer<JavaArchive> archive() {
        return jar -> jar.addClasses(Shipments.class, Invoices.class, AuditLog.class, LegacyImport.class, Reports.class,
                V1__Reports.class, V2__ShippingJava.class,
                SkipLegacy.class, MigrationLog.class, ShopApplication.class)
                .addAsResource("flyway/root.sql", "db/migration/__root/V1__log.sql")
                .addAsResource("flyway/shipping.sql", "db/migration/shipping/V1__shipping.sql")
                .addAsResource("flyway/billing.sql", "db/migration/billing/V1__billing.sql")
                .addAsResource("flyway/legacy.sql", "db/migration/legacy/V1__legacy.sql");
    }
}
