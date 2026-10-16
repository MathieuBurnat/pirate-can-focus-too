-- Les comptes gratuits « identifiant + mot de passe + code secret ». À part des pirates, comme les identités :
-- l'identifiant est privé (jamais montré), seul le pseudo du pirate apparaît dans l'équipage.
-- Mot de passe : PBKDF2-SHA256 salé ; code secret : empreinte SHA-256 (il est assez long pour ne pas être deviné).
CREATE TABLE credentials (
  pirate_id TEXT PRIMARY KEY REFERENCES pirates(id),
  login TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  password_salt TEXT NOT NULL,
  recovery_hash TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

-- Les essais ratés (mot de passe, code secret), par appareil (empreinte de l'IP) et par identifiant, pour freiner les devinettes.
CREATE TABLE auth_failures (
  ip_hash TEXT NOT NULL,
  login TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX auth_failures_ip ON auth_failures(ip_hash, created_at);
CREATE INDEX auth_failures_login ON auth_failures(login, created_at);
