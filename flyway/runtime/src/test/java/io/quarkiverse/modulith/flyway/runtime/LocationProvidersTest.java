package io.quarkiverse.modulith.flyway.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.junit.jupiter.api.Test;

import db.migration.billing.V2__Backfill;
import db.migration.shipping.V1__Shipments;

class LocationProvidersTest {

    private static final LoadableResource ROOT = new NamedResource("db/migration/__root/V1__log.sql");
    private static final LoadableResource BILLING = new NamedResource("db/migration/billing/V1__lines.sql");
    private static final LoadableResource BILLING_SPLIT = new NamedResource("db/migration/billing_split/V1__x.sql");
    private static final FixedResourceProvider ALL = new FixedResourceProvider(List.of(ROOT, BILLING, BILLING_SPLIT));

    @Test
    void keepsTheResourcesOfTheFolders() {
        // arrange
        LocationResourceProvider billing = new LocationResourceProvider(ALL, List.of("db/migration/billing/"));
        LocationResourceProvider audit = new LocationResourceProvider(ALL, List.of("db/migration/audit/"));

        // act
        Collection<LoadableResource> resources = billing.getResources("V", new String[] { ".sql" });
        LoadableResource byName = billing.getResource("db/migration/billing/V1__lines.sql");

        // assert
        assertThat(resources).containsExactly(BILLING);
        assertThat(byName).isSameAs(BILLING);
        assertThat(billing.hasFiles()).isTrue();
        assertThat(audit.hasFiles()).isFalse();
    }

    @Test
    void keepsTheJavaMigrationsOfTheFolders() {
        // arrange
        ClassProvider<JavaMigration> all = () -> List.of(V2__Backfill.class, V1__Shipments.class);

        // act
        Collection<Class<? extends JavaMigration>> classes = new LocationClassProvider(all,
                List.of("db/migration/billing/")).getClasses();

        // assert
        assertThat(classes).containsExactly(V2__Backfill.class);
    }
}
