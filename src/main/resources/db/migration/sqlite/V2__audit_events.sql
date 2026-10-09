CREATE TABLE IF NOT EXISTS tools_audit_events (
  id INTEGER PRIMARY KEY,
  subsystem TEXT NOT NULL,
  occurred_at INTEGER NOT NULL,
  message TEXT NOT NULL
);
