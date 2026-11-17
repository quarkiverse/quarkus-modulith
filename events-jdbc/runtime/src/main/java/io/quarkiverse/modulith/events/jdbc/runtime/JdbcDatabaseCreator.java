package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Optional;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

/**
 * Creates the synthetic {@link JdbcDatabase} bean from the {@link DatabaseType} name the build passes as the
 * {@code type} parameter, an empty name when the database is detected at startup.
 */
public class JdbcDatabaseCreator implements BeanCreator<JdbcDatabase> {

    public static final String TYPE = "type";

    @Override
    public JdbcDatabase create(SyntheticCreationalContext<JdbcDatabase> context) {
        String type = (String) context.getParams().get(TYPE);
        return new JdbcDatabase(Optional.of(type).filter(name -> !name.isEmpty()).map(DatabaseType::valueOf));
    }
}
