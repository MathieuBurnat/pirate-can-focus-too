-- Les pirates : un pseudo et ses réglages. Aucune donnée personnelle ici.
CREATE TABLE pirates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  -- 1 : visible dans les statistiques de l'équipage.
  public INTEGER NOT NULL DEFAULT 1,
  -- 1 : le compte a été supprimé, il ne reste qu'un « Ancien matelot ».
  former INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);

-- Les identités (email, Google), à part. Le lien va de l'identité vers le pirate, jamais l'inverse :
-- supprimer cette ligne suffit à effacer la personne derrière le pirate.
CREATE TABLE identities (
  pirate_id TEXT PRIMARY KEY REFERENCES pirates(id),
  email TEXT UNIQUE,
  google_sub TEXT UNIQUE,
  created_at TEXT NOT NULL
);

-- Jetons de session (seul leur empreinte SHA-256 est gardée).
CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  pirate_id TEXT NOT NULL REFERENCES pirates(id),
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL
);
CREATE INDEX sessions_pirate ON sessions(pirate_id);

-- Codes de connexion par email en attente.
CREATE TABLE email_codes (
  email TEXT PRIMARY KEY,
  code_hash TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL
);

-- Le journal de bord : une ligne par séance, verre ou traversée (même id que sur le téléphone).
-- Ajout seulement : ce qui est écrit est écrit, mouahaha.
CREATE TABLE logs (
  id TEXT PRIMARY KEY,
  pirate_id TEXT NOT NULL REFERENCES pirates(id),
  entry TEXT NOT NULL,
  at TEXT NOT NULL,
  note TEXT,
  synced_at TEXT NOT NULL
);
CREATE INDEX logs_pirate ON logs(pirate_id);
CREATE INDEX logs_at ON logs(at);

-- Le coffre : doublons et traversées.
CREATE TABLE chests (
  pirate_id TEXT PRIMARY KEY REFERENCES pirates(id),
  doubloons INTEGER NOT NULL DEFAULT 0,
  voyages INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL
);
