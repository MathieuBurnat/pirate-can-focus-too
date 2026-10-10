# L'API du capitaine (Cloudflare Worker + D1)

Sauvegarde des comptes, du journal de bord et du coffre (issue #8).
Sans compte, l'app n'appelle jamais cette API : tout reste sur le téléphone.

## Les tables

- `pirates` : pseudo, visibilité publique. Aucune donnée personnelle.
- `identities` : email ou identifiant Google, rattaché à un pirate. **Séparé** des statistiques.
- `logs`, `chests` : journal de bord et coffre, rattachés au pirate.
- `sessions`, `email_codes` : jetons et codes de connexion (seules leurs empreintes SHA-256 sont gardées).

Supprimer un compte efface l'identité, les sessions et les petits mots du journal. Le pirate devient
« Ancien matelot n°123 » et ses statistiques restent au classement.

## Les routes

| Route | Jeton | Rôle |
|---|---|---|
| `POST /auth/email/start` `{email}` | | Envoie un code à 6 chiffres (valable 10 min, 1 envoi par minute par adresse, 10 par heure par IP) |
| `POST /auth/email/verify` `{email, code, name?}` | | Connexion ; crée le pirate au premier passage (5 essais par code) |
| `POST /auth/google` `{idToken, name?}` | | Connexion avec un ID token Google |
| `POST /auth/logout` | ✓ | Révoque le jeton |
| `GET /me` | ✓ | Pseudo, visibilité, type de compte |
| `PATCH /me` `{name?, public?}` | ✓ | Change le pseudo ou la visibilité publique |
| `DELETE /me` | ✓ | Supprime le compte (anonymisation) |
| `POST /sync` `{logs, chest?}` | ✓ | Envoie les lignes (avec `deleted` pour les ratures) et le coffre, reçoit tout en retour |
| `GET /crew` | | Les pirates publics et leur journal du dernier mois |

Le jeton se passe en `Authorization: Bearer <jeton>`.

## Lancer en local

```sh
cd backend
npm install
cp .dev.vars.example .dev.vars        # les codes email s'affichent dans la console
npm run db:migrate:local
npm run dev
npm test                              # tests dans le runtime Workers (Miniflare)
```

## Mettre en ligne

```sh
npx wrangler login
npx wrangler d1 create pirate-db      # copier l'id dans wrangler.jsonc (database_id)
npm run db:migrate:remote
npx wrangler secret put RESEND_API_KEY
npm run deploy
```

Avant le déploiement, renseigner dans `wrangler.jsonc` :
- `GOOGLE_CLIENT_IDS` : les identifiants OAuth (client Web utilisé par Credential Manager côté Android) ;
- `EMAIL_FROM` : une adresse d'un domaine vérifié chez Resend.
