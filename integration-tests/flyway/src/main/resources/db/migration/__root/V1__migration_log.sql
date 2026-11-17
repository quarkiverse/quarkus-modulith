CREATE TABLE migration_log (id SERIAL PRIMARY KEY, entry TEXT NOT NULL);
INSERT INTO migration_log (entry) VALUES ('root');
