# L'API du capitaine (Cloudflare Worker + D1)

En ligne sur https://yaawwwk-pirate-can-focus-too.helveticademia.ch

Sauvegarde des comptes, du journal de bord et du coffre (issue #8).
Sans compte, l'app n'appelle jamais cette API : tout reste sur le téléphone.

## Les tables

- `pirates` : pseudo, visibilité publique. Aucune donnée personnelle.
- `credentials` : le compte gratuit utilisé par l'app : email servant d'identifiant (jamais vérifié, aucun mail envoyé),
  mot de passe (PBKDF2-SHA256 salé) et empreinte du code secret. **Séparé** des statistiques, l'email n'est jamais montré.
- `identities` : email ou identifiant Google, rattaché à un pirate (routes gardées, plus utilisées par l'app).
- `auth_failures` : essais ratés par appareil (empreinte de l'IP) et par identifiant, pour freiner les devinettes.
- `exploits`, `chests` : journal de bord (chaque séance, verre ou traversée) et coffre, rattachés au pirate.
- `sessions`, `email_codes` : jetons et codes de connexion (seules leurs empreintes SHA-256 sont gardées).

Supprimer un compte efface l'identité, les sessions et les petits mots du journal. Le pirate devient
« Ancien matelot n°123 » et ses statistiques restent au classement.

## Les routes

| Route | Jeton | Rôle |
|---|---|---|
| `POST /auth/register` `{login, password, name?}` | | Crée le compte ; renvoie le jeton et le **code secret**, montré une seule fois (identifiant pris : 409) |
| `POST /auth/login` `{login, password}` | | Connexion (même erreur si l'identifiant ou le mot de passe est faux ; 10 échecs par IP en 10 min ou par identifiant en 1 h : 429) |
| `POST /auth/recover` `{login, code, newPassword}` | | Mot de passe oublié : le code secret remet un mot de passe, un nouveau code est renvoyé, les autres sessions sont fermées |
| `POST /me/recovery` | ✓ | Nouveau code secret (l'ancien ne marche plus) |
| `POST /me/password` `{current, new}` | ✓ | Change le mot de passe |
| `POST /auth/email/start` `{email}` | | Envoie un code à 6 chiffres (valable 10 min, 1 envoi par minute par adresse, 10 par heure par IP) |
| `POST /auth/email/verify` `{email, code, name?}` | | Connexion ; crée le pirate au premier passage (5 essais par code) |
| `POST /auth/google` `{idToken, name?}` | | Connexion avec un ID token Google |
| `POST /auth/logout` | ✓ | Révoque le jeton |
| `GET /me` | ✓ | Pseudo, visibilité, type de compte |
| `PATCH /me` (ou `POST /me`) `{name?, public?}` | ✓ | Change le pseudo ou la visibilité publique |
| `DELETE /me` | ✓ | Supprime le compte (anonymisation) |
| `POST /sync` `{logs, chest?}` | ✓ | Envoie les nouvelles lignes et le coffre, reçoit tout en retour. Journal en ajout seulement : rien n'est modifié ni effacé |
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
npx wrangler d1 create pirate-can-focus-db      # copier l'id dans wrangler.jsonc (database_id)
npm run db:migrate:remote                       # applique les nouvelles migrations (0003_credentials...)
npx wrangler secret put BREVO_API_KEY       # facultatif : seulement pour les codes par email
npm run deploy
```

Avant le déploiement, renseigner dans `wrangler.jsonc` :
- `GOOGLE_CLIENT_IDS` : les identifiants OAuth (client Web utilisé par Credential Manager côté Android) ;
- `EMAIL_FROM` : « Nom <adresse> », adresse d'un domaine vérifié chez Brevo (ou Resend).
