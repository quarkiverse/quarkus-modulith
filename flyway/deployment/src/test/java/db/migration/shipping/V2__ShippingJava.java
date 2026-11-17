package db.migration.shipping;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__ShippingJava extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("INSERT INTO MIGRATION_LOG (ENTRY) VALUES ('shipping-java')");
        }
    }
}
