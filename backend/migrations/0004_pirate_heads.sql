-- La tête du pirate (#18) : l'une des têtes prédéfinies de l'app (0 = celle de départ).
ALTER TABLE pirates ADD COLUMN head INTEGER NOT NULL DEFAULT 0;
-- Les têtes débloquées au niveau 5 (3 au choix), séparées par des virgules. Au niveau 10, toutes sont permises.
ALTER TABLE pirates ADD COLUMN heads TEXT NOT NULL DEFAULT '';
