CREATE TABLE event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
INSERT INTO event_publication (id, listener_id, event_type, serialized_event, publication_date, status, completion_attempts)
VALUES ('00000000-0000-0000-0000-000000000001', 'existing.listener',
        'io.quarkiverse.modulith.it.eventsjdbc.greeting.GreetingPublished', '{"message":"kept"}',
        TIMESTAMP WITH TIME ZONE '2026-01-01 10:00:00+00', 'PUBLISHED', 0);
