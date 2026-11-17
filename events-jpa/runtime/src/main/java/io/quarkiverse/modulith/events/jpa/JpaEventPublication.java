package io.quarkiverse.modulith.events.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A row of the {@code event_publication} table, Spring Modulith's {@code EVENT_PUBLICATION}. Besides Spring's index on
 * the completion date, a generated schema gets an index for the queries every resubmission and outbox run makes:
 * incomplete publications of one listener, in keyset order.
 */
@Entity(name = "JpaEventPublication")
@Table(name = "event_publication", indexes = {
        @Index(name = "event_publication_by_completion_date_idx", columnList = "completion_date"),
        @Index(name = "event_publication_by_listener_idx", columnList = "completion_date, listener_id, publication_date, id")
})
public class JpaEventPublication extends AbstractJpaEventPublication {
}
