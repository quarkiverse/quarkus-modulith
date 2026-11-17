package io.quarkiverse.modulith.events.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A row of the {@code event_publication_archive} table, Spring Modulith's {@code EVENT_PUBLICATION_ARCHIVE}. Mapped
 * only with {@code quarkus.modulith.events.completion-mode=archive}: completed publications move here.
 */
@Entity(name = "ArchivedJpaEventPublication")
@Table(name = "event_publication_archive")
public class ArchivedJpaEventPublication extends AbstractJpaEventPublication {
}
