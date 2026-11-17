package io.quarkiverse.modulith.events.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkiverse.modulith.events.EventPublication.Status;

/**
 * The columns of the event publication tables. The names are the ones Spring Modulith uses, so an application moving
 * from Spring Modulith can keep its tables.
 */
@MappedSuperclass
public abstract class AbstractJpaEventPublication {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "listener_id", nullable = false, length = 512)
    public String listenerId;

    @Column(name = "event_type", nullable = false, length = 512)
    public String eventType;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "serialized_event", nullable = false)
    public String serializedEvent;

    @Column(name = "publication_date", nullable = false)
    public Instant publicationDate;

    @Column(name = "completion_date")
    public Instant completionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    public Status status;

    @Column(name = "completion_attempts")
    public Integer completionAttempts;

    @Column(name = "last_resubmission_date")
    public Instant lastResubmissionDate;
}
