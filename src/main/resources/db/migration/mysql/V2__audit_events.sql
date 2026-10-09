-- Pierwsza migracja po dotychczasowym schemacie, bez modyfikacji danych graczy.
CREATE TABLE IF NOT EXISTS tools_audit_events (
  id BIGINT NOT NULL PRIMARY KEY,
  subsystem VARCHAR(64) NOT NULL,
  occurred_at BIGINT NOT NULL,
  message VARCHAR(512) NOT NULL
);
