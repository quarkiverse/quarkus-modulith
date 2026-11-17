package io.quarkiverse.modulith.events.neo4j.deployment;

import java.util.List;

import org.neo4j.driver.Driver;

public final class Indexes {

    private Indexes() {
    }

    public static List<String> names(Driver driver) {
        return show(driver, "INDEXES");
    }

    public static List<String> constraints(Driver driver) {
        return show(driver, "CONSTRAINTS");
    }

    private static List<String> show(Driver driver, String schemaObjects) {
        return driver.executableQuery("SHOW " + schemaObjects + " YIELD name RETURN name").execute().records().stream()
                .map(record -> record.get("name").asString())
                .toList();
    }
}
