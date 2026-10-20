-- Les envois de codes, par appareil (empreinte de l'IP, jamais l'IP elle-même), pour freiner les abus.
CREATE TABLE email_sends (
  ip_hash TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX email_sends_ip ON email_sends(ip_hash, created_at);
