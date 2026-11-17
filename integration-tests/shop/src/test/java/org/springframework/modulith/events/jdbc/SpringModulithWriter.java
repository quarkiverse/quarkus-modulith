package org.springframework.modulith.events.jdbc;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.core.DefaultEventPublicationRegistry;
import org.springframework.modulith.events.core.PublicationTargetIdentifier;
import org.springframework.modulith.events.core.TargetEventPublication;
import org.springframework.modulith.events.jackson.SpringSerializers;
import org.springframework.modulith.events.support.CompletionMode;

/**
 * Stores publications with Spring Modulith 2.1.2's own registry, JDBC repository (current table structure) and Jackson
 * serializer, exactly as a Spring Modulith application would before moving to Quarkus. Lives in Spring's package
 * because the repository and its settings are package-private.
 */
public final class SpringModulithWriter {

    private SpringModulithWriter() {
    }

    public static List<UUID> store(DataSource dataSource, Object event, List<String> listenerIds) {
        JdbcRepositorySettings settings = new JdbcRepositorySettings(DatabaseType.POSTGRES, CompletionMode.UPDATE,
                new JdbcConfigurationProperties(new JdbcConfigurationProperties.SchemaInitialization(false), null, false));
        JdbcEventPublicationRepositoryV2 repository = new JdbcEventPublicationRepositoryV2(new JdbcTemplate(dataSource),
                SpringSerializers.jackson(), settings);
        repository.setBeanClassLoader(SpringModulithWriter.class.getClassLoader());
        DefaultEventPublicationRegistry registry = new DefaultEventPublicationRegistry(repository, Clock.systemUTC());
        return registry.store(event, listenerIds.stream().map(PublicationTargetIdentifier::of)).stream()
                .map(TargetEventPublication::getIdentifier).toList();
    }

    /**
     * @return {@code listenerId=status} for every stored publication, ordered by listener id
     */
    public static Stream<String> describe(DataSource dataSource) {
        return new JdbcTemplate(dataSource)
                .queryForList("select listener_id, status from event_publication order by listener_id")
                .stream().map(row -> row.get("listener_id") + "=" + row.get("status"));
    }
}
