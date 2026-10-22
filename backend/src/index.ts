import { hashPassword, normalizeRecoveryCode, randomCode, randomRecoveryCode, randomToken, sameText, sha256 } from "./crypto";
import { sendLoginCode } from "./email";
import type { Env } from "./env";
import { verifyGoogleIdToken } from "./google";
import { ALL_HEADS_LEVEL, HEAD_COUNT, HEADS_AT_LEVEL_5, HEADS_LEVEL, headSlots, levelOf, parseHeads, STARTER_HEADS } from "./levels";
import { cleanPirateName, formerPirateName, randomPirateName } from "./names";

/** Les lignes que le journal du téléphone sait écrire (enum Entry côté app). */
const ENTRIES = new Set(["MEGA_SEANCE", "GRIMPE", "ABDOS", "BIERE", "COCKTAIL", "VIN", "TRAVERSEE"]);

const SESSION_DAYS = 180;
const CODE_MINUTES = 10;
const CODE_COOLDOWN_SECONDS = 60;
const CODE_MAX_ATTEMPTS = 5;
/** Codes envoyés au plus par appareil (IP) et par heure, toutes adresses confondues. */
const CODES_PER_IP_PER_HOUR = 10;
/**
 * Essais ratés (mot de passe, code secret) tolérés sur une heure, par identifiant et par appareil. Au-delà, la cale est
 * verrouillée 2 minutes après le dernier échec, puis une minute de plus à chaque nouvel échec.
 */
const AUTH_FAILURES_PER_LOGIN = 5;
const AUTH_FAILURES_PER_IP = 10;
const LOCK_FIRST_MINUTES = 2;
const PASSWORD_MIN = 8;
const PASSWORD_MAX = 200;
const SYNC_MAX_LOGS = 500;
const CREW_DAYS = 31;

class HttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

const json = (data: unknown, status = 200) => Response.json(data, { status });
const nowIso = () => new Date().toISOString();
const daysFromNow = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString();

async function body(request: Request): Promise<Record<string, unknown>> {
  try {
    const data = await request.json();
    if (data && typeof data === "object" && !Array.isArray(data)) return data as Record<string, unknown>;
  } catch {
    // corps illisible : traité comme vide
  }
  throw new HttpError(400, "Corps JSON attendu");
}

// --- Sessions --------------------------------------------------------------

async function openSession(env: Env, pirateId: string) {
  const token = randomToken();
  await env.DB.prepare("INSERT INTO sessions (token_hash, pirate_id, created_at, expires_at) VALUES (?, ?, ?, ?)")
    .bind(await sha256(token), pirateId, nowIso(), daysFromNow(SESSION_DAYS))
    .run();
  return token;
}

/** Le pirate derrière le jeton « Authorization: Bearer ... », ou une erreur 401. */
async function pirateOf(env: Env, request: Request): Promise<string> {
  const token = /^Bearer (.+)$/.exec(request.headers.get("Authorization") ?? "")?.[1];
  if (!token) throw new HttpError(401, "Jeton manquant");
  const row = await env.DB.prepare("SELECT pirate_id FROM sessions WHERE token_hash = ? AND expires_at > ?")
    .bind(await sha256(token), nowIso())
    .first<{ pirate_id: string }>();
  if (!row) throw new HttpError(401, "Jeton inconnu ou expiré");
  return row.pirate_id;
}

/** Trouve le pirate lié à une identité, ou en crée un (avec le pseudo choisi, ou tiré au sort). */
async function pirateForIdentity(env: Env, column: "email" | "google_sub", value: string, email: string | null, name: unknown) {
  const existing = await env.DB.prepare(`SELECT pirate_id FROM identities WHERE ${column} = ?`).bind(value).first<{ pirate_id: string }>();
  if (existing) return { pirateId: existing.pirate_id, created: false };

  const pirateId = crypto.randomUUID();
  const now = nowIso();
  await env.DB.batch([
    env.DB.prepare("INSERT INTO pirates (id, name, created_at) VALUES (?, ?, ?)").bind(pirateId, cleanPirateName(name) ?? randomPirateName(), now),
    env.DB.prepare("INSERT INTO identities (pirate_id, email, google_sub, created_at) VALUES (?, ?, ?, ?)").bind(
      pirateId,
      email,
      column === "google_sub" ? value : null,
      now,
    ),
  ]);
  return { pirateId, created: true };
}

async function welcome(env: Env, pirateId: string, created: boolean) {
  return json({ token: await openSession(env, pirateId), created, me: await me(env, pirateId) });
}

// --- Connexion ---------------------------------------------------------------

async function googleLogin(env: Env, request: Request) {
  const data = await body(request);
  if (typeof data.idToken !== "string") throw new HttpError(400, "idToken attendu");
  const clientIds = env.GOOGLE_CLIENT_IDS.split(",").map((id) => id.trim()).filter(Boolean);
  const user = await verifyGoogleIdToken(data.idToken, clientIds);
  if (!user) throw new HttpError(401, "Jeton Google refusé");
  const { pirateId, created } = await pirateForIdentity(env, "google_sub", user.sub, user.email?.toLowerCase() ?? null, data.name);
  return welcome(env, pirateId, created);
}

function cleanEmail(value: unknown): string {
  const email = typeof value === "string" ? value.trim().toLowerCase() : "";
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new HttpError(400, "Adresse email invalide");
  return email;
}

/** Freine qui voudrait arroser des inconnus de codes : 10 envois par heure et par IP. */
async function checkSendQuota(env: Env, request: Request) {
  const ipHash = await sha256(`ip:${request.headers.get("CF-Connecting-IP") ?? "inconnue"}`);
  const hourAgo = new Date(Date.now() - 3_600_000).toISOString();
  const [, recent] = await env.DB.batch([
    env.DB.prepare("DELETE FROM email_sends WHERE created_at < ?").bind(hourAgo),
    env.DB.prepare("SELECT COUNT(*) AS n FROM email_sends WHERE ip_hash = ?").bind(ipHash),
  ]);
  if (((recent.results[0] as { n: number }).n ?? 0) >= CODES_PER_IP_PER_HOUR) {
    throw new HttpError(429, "Trop de pigeons voyageurs envoyés, reviens dans une heure");
  }
  await env.DB.prepare("INSERT INTO email_sends (ip_hash, created_at) VALUES (?, ?)").bind(ipHash, nowIso()).run();
}

async function emailStart(env: Env, request: Request) {
  const email = cleanEmail((await body(request)).email);
  const pending = await env.DB.prepare("SELECT created_at FROM email_codes WHERE email = ?").bind(email).first<{ created_at: string }>();
  if (pending && Date.now() - Date.parse(pending.created_at) < CODE_COOLDOWN_SECONDS * 1000) {
    throw new HttpError(429, "Un code vient déjà de partir, patience moussaillon");
  }
  await checkSendQuota(env, request);
  const code = randomCode();
  await env.DB.prepare(
    `INSERT INTO email_codes (email, code_hash, attempts, created_at, expires_at) VALUES (?, ?, 0, ?, ?)
     ON CONFLICT(email) DO UPDATE SET code_hash = excluded.code_hash, attempts = 0, created_at = excluded.created_at, expires_at = excluded.expires_at`,
  )
    .bind(email, await sha256(`${email}:${code}`), nowIso(), new Date(Date.now() + CODE_MINUTES * 60_000).toISOString())
    .run();
  if (!(await sendLoginCode(env, email, code))) throw new HttpError(503, "Le pigeon voyageur n'a pas pu partir");
  return json({ sent: true });
}

async function emailVerify(env: Env, request: Request) {
  const data = await body(request);
  const email = cleanEmail(data.email);
  const code = typeof data.code === "string" ? data.code.trim() : "";
  const pending = await env.DB.prepare("SELECT code_hash, attempts, expires_at FROM email_codes WHERE email = ?")
    .bind(email)
    .first<{ code_hash: string; attempts: number; expires_at: string }>();
  if (!pending || pending.expires_at < nowIso() || pending.attempts >= CODE_MAX_ATTEMPTS) {
    throw new HttpError(401, "Code expiré : demandes-en un nouveau");
  }
  if (!sameText(await sha256(`${email}:${code}`), pending.code_hash)) {
    await env.DB.prepare("UPDATE email_codes SET attempts = attempts + 1 WHERE email = ?").bind(email).run();
    throw new HttpError(401, "Mauvais code");
  }
  await env.DB.prepare("DELETE FROM email_codes WHERE email = ?").bind(email).run();
  const { pirateId, created } = await pirateForIdentity(env, "email", email, email, data.name);
  return welcome(env, pirateId, created);
}

// --- Identifiant, mot de passe et code secret -----------------------------------

/**
 * Identifiant privé : une adresse email (en minuscules), qui ne sert qu'à se connecter.
 * Aucun mail n'y est jamais envoyé, et elle n'est jamais montrée aux autres pirates.
 */
function cleanLogin(value: unknown): string {
  const login = typeof value === "string" ? value.trim().toLowerCase() : "";
  if (login.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(login)) throw new HttpError(400, "Adresse email invalide, matelot");
  return login;
}

function cleanPassword(value: unknown): string {
  const password = typeof value === "string" ? value : "";
  if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
    throw new HttpError(400, `Mot de passe de ${PASSWORD_MIN} caractères minimum, matelot`);
  }
  return password;
}

const recoveryHash = async (code: string) => sha256(`recovery:${normalizeRecoveryCode(code)}`);
const ipHashOf = (request: Request) => sha256(`ip:${request.headers.get("CF-Connecting-IP") ?? "inconnue"}`);

/** Fin du verrou après [failures] échecs (le dernier à [last]), ou null s'il n'y en a pas : 2 min, puis +1 min par échec. */
function lockedUntil(failures: number, last: string | null, threshold: number): number | null {
  if (failures < threshold || !last) return null;
  return Date.parse(last) + (LOCK_FIRST_MINUTES + failures - threshold) * 60_000;
}

/** Trop d'essais ratés sur cet identifiant ou depuis cet appareil : on ferme le pont-levis quelques minutes. */
async function checkAuthFailures(env: Env, request: Request, login: string) {
  const hourAgo = new Date(Date.now() - 3_600_000).toISOString();
  const [, byIp, byLogin] = await env.DB.batch([
    env.DB.prepare("DELETE FROM auth_failures WHERE created_at < ?").bind(hourAgo),
    env.DB.prepare("SELECT COUNT(*) AS n, MAX(created_at) AS last FROM auth_failures WHERE ip_hash = ?").bind(await ipHashOf(request)),
    env.DB.prepare("SELECT COUNT(*) AS n, MAX(created_at) AS last FROM auth_failures WHERE login = ?").bind(login),
  ]);
  const row = (result: D1Result) => result.results[0] as { n: number; last: string | null };
  const until = Math.max(
    lockedUntil(row(byIp).n, row(byIp).last, AUTH_FAILURES_PER_IP) ?? 0,
    lockedUntil(row(byLogin).n, row(byLogin).last, AUTH_FAILURES_PER_LOGIN) ?? 0,
  );
  if (until > Date.now()) {
    const minutes = Math.ceil((until - Date.now()) / 60_000);
    throw new HttpError(
      429,
      `Trop d'essais ratés : le capitaine verrouille la cale. Reviens dans ${minutes} minute${minutes > 1 ? "s" : ""}, matelot !`,
    );
  }
}

async function recordAuthFailure(env: Env, request: Request, login: string) {
  await env.DB.prepare("INSERT INTO auth_failures (ip_hash, login, created_at) VALUES (?, ?, ?)")
    .bind(await ipHashOf(request), login, nowIso())
    .run();
}

interface CredentialRow {
  pirate_id: string;
  password_hash: string;
  password_salt: string;
  recovery_hash: string;
}

const credentialsOf = (env: Env, login: string) =>
  env.DB.prepare("SELECT pirate_id, password_hash, password_salt, recovery_hash FROM credentials WHERE login = ?")
    .bind(login)
    .first<CredentialRow>();

/** Nouveau compte gratuit : le code secret n'est montré qu'une fois, ici. */
async function register(env: Env, request: Request) {
  const data = await body(request);
  const login = cleanLogin(data.login);
  const password = cleanPassword(data.password);
  if (await env.DB.prepare("SELECT 1 FROM credentials WHERE login = ?").bind(login).first()) {
    throw new HttpError(409, "Cet identifiant est déjà pris par un autre pirate");
  }
  const pirateId = crypto.randomUUID();
  const recoveryCode = randomRecoveryCode();
  const { hash, salt } = await hashPassword(password);
  const now = nowIso();
  await env.DB.batch([
    env.DB.prepare("INSERT INTO pirates (id, name, created_at) VALUES (?, ?, ?)").bind(pirateId, cleanPirateName(data.name) ?? randomPirateName(), now),
    env.DB.prepare(
      `INSERT INTO credentials (pirate_id, login, password_hash, password_salt, recovery_hash, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    ).bind(pirateId, login, hash, salt, await recoveryHash(recoveryCode), now, now),
  ]);
  return json({ token: await openSession(env, pirateId), created: true, recoveryCode, me: await me(env, pirateId) });
}

/** Connexion : on ne dit jamais si c'est l'identifiant ou le mot de passe qui cloche. */
async function passwordLogin(env: Env, request: Request) {
  const data = await body(request);
  const login = typeof data.login === "string" ? data.login.trim().toLowerCase() : "";
  const password = typeof data.password === "string" ? data.password : "";
  await checkAuthFailures(env, request, login);
  const row = login ? await credentialsOf(env, login) : null;
  const ok = row !== null && sameText((await hashPassword(password, row.password_salt)).hash, row.password_hash);
  if (!row || !ok) {
    await recordAuthFailure(env, request, login);
    throw new HttpError(401, "Identifiant ou mot de passe incorrect");
  }
  return welcome(env, row.pirate_id, false);
}

/** Mot de passe oublié : le code secret ouvre la cale une fois, puis un nouveau code le remplace. */
async function recover(env: Env, request: Request) {
  const data = await body(request);
  const login = typeof data.login === "string" ? data.login.trim().toLowerCase() : "";
  const code = typeof data.code === "string" ? data.code : "";
  const password = cleanPassword(data.newPassword);
  await checkAuthFailures(env, request, login);
  const row = login ? await credentialsOf(env, login) : null;
  if (!row || !sameText(await recoveryHash(code), row.recovery_hash)) {
    await recordAuthFailure(env, request, login);
    throw new HttpError(401, "Identifiant ou code secret incorrect");
  }
  const recoveryCode = randomRecoveryCode();
  const { hash, salt } = await hashPassword(password);
  await env.DB.batch([
    env.DB.prepare("UPDATE credentials SET password_hash = ?, password_salt = ?, recovery_hash = ?, updated_at = ? WHERE pirate_id = ?").bind(
      hash,
      salt,
      await recoveryHash(recoveryCode),
      nowIso(),
      row.pirate_id,
    ),
    // Les autres appareils sont déconnectés : quelqu'un d'autre connaissait peut-être l'ancien mot de passe.
    env.DB.prepare("DELETE FROM sessions WHERE pirate_id = ?").bind(row.pirate_id),
  ]);
  return json({ token: await openSession(env, row.pirate_id), created: false, recoveryCode, me: await me(env, row.pirate_id) });
}

/** Nouveau code secret (l'ancien ne marche plus), montré une seule fois. */
async function newRecoveryCode(env: Env, pirateId: string) {
  const recoveryCode = randomRecoveryCode();
  const { meta } = await env.DB.prepare("UPDATE credentials SET recovery_hash = ?, updated_at = ? WHERE pirate_id = ?")
    .bind(await recoveryHash(recoveryCode), nowIso(), pirateId)
    .run();
  if (meta.changes === 0) throw new HttpError(400, "Ce compte n'a pas de code secret");
  return json({ recoveryCode });
}

async function changePassword(env: Env, request: Request, pirateId: string) {
  const data = await body(request);
  const password = cleanPassword(data.new);
  const row = await env.DB.prepare("SELECT login, password_hash, password_salt FROM credentials WHERE pirate_id = ?")
    .bind(pirateId)
    .first<{ login: string; password_hash: string; password_salt: string }>();
  if (!row) throw new HttpError(400, "Ce compte n'a pas de mot de passe");
  await checkAuthFailures(env, request, row.login);
  const current = typeof data.current === "string" ? data.current : "";
  if (!sameText((await hashPassword(current, row.password_salt)).hash, row.password_hash)) {
    await recordAuthFailure(env, request, row.login);
    throw new HttpError(401, "Mot de passe actuel incorrect");
  }
  const { hash, salt } = await hashPassword(password);
  await env.DB.prepare("UPDATE credentials SET password_hash = ?, password_salt = ?, updated_at = ? WHERE pirate_id = ?")
    .bind(hash, salt, nowIso(), pirateId)
    .run();
  return json({ ok: true });
}

// --- Mon compte --------------------------------------------------------------

interface MeRow {
  name: string;
  public: number;
  email: string | null;
  google_sub: string | null;
  login: string | null;
  head: number;
  heads: string;
  doubloons: number | null;
}

async function meRow(env: Env, pirateId: string): Promise<MeRow> {
  const row = await env.DB.prepare(
    `SELECT p.name, p.public, p.head, p.heads, i.email, i.google_sub, c.login, ch.doubloons FROM pirates p
     LEFT JOIN identities i ON i.pirate_id = p.id LEFT JOIN credentials c ON c.pirate_id = p.id
     LEFT JOIN chests ch ON ch.pirate_id = p.id WHERE p.id = ?`,
  )
    .bind(pirateId)
    .first<MeRow>();
  if (!row) throw new HttpError(404, "Pirate introuvable");
  return row;
}

async function me(env: Env, pirateId: string) {
  const row = await meRow(env, pirateId);
  const kind = row.login ? "password" : row.google_sub ? "google" : "email";
  return {
    name: row.name,
    public: row.public === 1,
    // La tête portée, celles débloquées au niveau 5, et le niveau (tiré des doublons du coffre).
    head: row.head,
    heads: parseHeads(row.heads),
    level: levelOf(row.doubloons ?? 0),
    // google : un compte Google est lié (connexion Google possible), qu'il soit l'identité principale ou ajouté ensuite.
    account: { kind, email: row.email, login: row.login, google: row.google_sub !== null },
  };
}

/**
 * Changer de tête (#18) : la tête de départ et les têtes déjà débloquées sont toujours permises. Au niveau 5,
 * une nouvelle tête se débloque tant qu'il reste une des 3 places ; au niveau 10, toutes sont permises.
 */
async function chooseHead(env: Env, request: Request, pirateId: string) {
  const head = (await body(request)).head;
  if (typeof head !== "number" || !Number.isInteger(head) || head < 0 || head >= HEAD_COUNT) {
    throw new HttpError(400, `Tête de 0 à ${HEAD_COUNT - 1} attendue`);
  }
  const row = await meRow(env, pirateId);
  const unlocked = parseHeads(row.heads);
  const level = levelOf(row.doubloons ?? 0);
  if (!STARTER_HEADS.has(head) && !unlocked.includes(head)) {
    if (level < HEADS_LEVEL) throw new HttpError(403, `Changer de tête, c'est au niveau ${HEADS_LEVEL}, moussaillon !`);
    if (unlocked.length >= headSlots(level)) {
      throw new HttpError(403, `Tes ${HEADS_AT_LEVEL_5} têtes sont choisies : les autres attendront le niveau ${ALL_HEADS_LEVEL} !`);
    }
    // Au niveau 10, inutile de compter : toutes les têtes sont permises.
    if (level < ALL_HEADS_LEVEL) unlocked.push(head);
  }
  await env.DB.prepare("UPDATE pirates SET head = ?, heads = ? WHERE id = ?").bind(head, unlocked.join(","), pirateId).run();
  return json(await me(env, pirateId));
}

/** Lier un compte Google au pirate connecté : ensuite, « CONTINUER AVEC GOOGLE » retombe sur ce même pirate. */
async function linkGoogle(env: Env, request: Request, pirateId: string) {
  const data = await body(request);
  if (typeof data.idToken !== "string") throw new HttpError(400, "idToken attendu");
  const clientIds = env.GOOGLE_CLIENT_IDS.split(",").map((id) => id.trim()).filter(Boolean);
  const user = await verifyGoogleIdToken(data.idToken, clientIds);
  if (!user) throw new HttpError(401, "Jeton Google refusé");

  const owner = await env.DB.prepare("SELECT pirate_id FROM identities WHERE google_sub = ?").bind(user.sub).first<{ pirate_id: string }>();
  if (owner && owner.pirate_id !== pirateId) {
    throw new HttpError(409, "Ce compte Google navigue déjà sous un autre pavillon ! Un seul pirate par compte Google, matelot.");
  }
  if (!owner) {
    const mine = await env.DB.prepare("SELECT google_sub FROM identities WHERE pirate_id = ?").bind(pirateId).first<{ google_sub: string | null }>();
    if (mine?.google_sub) throw new HttpError(409, "Ton pirate a déjà un compte Google lié : délie-le d'abord.");
    const email = user.email?.toLowerCase() ?? null;
    // L'email Google n'est gardé que s'il n'est pas déjà celui d'une autre identité (il est unique).
    const emailFree = email === null || !(await env.DB.prepare("SELECT 1 FROM identities WHERE email = ? AND pirate_id <> ?").bind(email, pirateId).first());
    if (mine) {
      await env.DB.prepare("UPDATE identities SET google_sub = ?, email = COALESCE(email, ?) WHERE pirate_id = ?")
        .bind(user.sub, emailFree ? email : null, pirateId)
        .run();
    } else {
      await env.DB.prepare("INSERT INTO identities (pirate_id, email, google_sub, created_at) VALUES (?, ?, ?, ?)")
        .bind(pirateId, emailFree ? email : null, user.sub, nowIso())
        .run();
    }
  }
  return json(await me(env, pirateId));
}

/** Délier Google, seulement si le pirate garde une autre façon de monter à bord (identifiant et mot de passe). */
async function unlinkGoogle(env: Env, pirateId: string) {
  if (!(await env.DB.prepare("SELECT 1 FROM credentials WHERE pirate_id = ?").bind(pirateId).first())) {
    throw new HttpError(400, "Sans mot de passe, Google est ta seule passerelle : impossible de la retirer.");
  }
  await env.DB.prepare("DELETE FROM identities WHERE pirate_id = ?").bind(pirateId).run();
  return json(await me(env, pirateId));
}

async function updateMe(env: Env, request: Request, pirateId: string) {
  const data = await body(request);
  if (data.name !== undefined) {
    const name = cleanPirateName(data.name);
    if (!name) throw new HttpError(400, "Pseudo de 2 à 48 caractères attendu");
    await env.DB.prepare("UPDATE pirates SET name = ? WHERE id = ?").bind(name, pirateId).run();
  }
  if (data.public !== undefined) {
    if (typeof data.public !== "boolean") throw new HttpError(400, "public doit valoir true ou false");
    await env.DB.prepare("UPDATE pirates SET public = ? WHERE id = ?").bind(data.public ? 1 : 0, pirateId).run();
  }
  return json(await me(env, pirateId));
}

/**
 * Suppression du compte : l'identité (email, Google, identifiant et mot de passe) et les sessions disparaissent, les petits mots
 * du journal sont effacés, et le pirate devient un « Ancien matelot » dont les stats restent au classement.
 */
async function deleteMe(env: Env, pirateId: string) {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM identities WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("DELETE FROM credentials WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("DELETE FROM sessions WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("UPDATE exploits SET note = NULL WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("UPDATE pirates SET name = ?, former = 1 WHERE id = ?").bind(formerPirateName(), pirateId),
  ]);
  return json({ deleted: true });
}

async function logout(env: Env, request: Request) {
  const token = /^Bearer (.+)$/.exec(request.headers.get("Authorization") ?? "")?.[1] ?? "";
  await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(await sha256(token)).run();
  return json({ ok: true });
}

// --- Synchronisation ---------------------------------------------------------

interface LogLine {
  id: string;
  entry: string;
  at: string;
  note: string | null;
}

function cleanLog(value: unknown): LogLine {
  const log = (value ?? {}) as Record<string, unknown>;
  const { id, entry, at, note } = log;
  if (typeof id !== "string" || id.length === 0 || id.length > 64) throw new HttpError(400, "Ligne sans id valide");
  if (typeof entry !== "string" || !ENTRIES.has(entry)) throw new HttpError(400, `Entrée inconnue : ${String(entry)}`);
  if (typeof at !== "string" || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(at) || at.length > 40) throw new HttpError(400, "Date invalide");
  if (note != null && (typeof note !== "string" || note.length > 80)) throw new HttpError(400, "Petit mot de 80 caractères maximum");
  return { id, entry, at, note: (note as string | null | undefined) || null };
}

/**
 * Le téléphone envoie ses lignes et son coffre ; il reçoit en retour tout ce que le Worker connaît de ce pirate.
 * Le journal est en ajout seulement : une ligne déjà connue n'est jamais modifiée ni effacée.
 */
async function sync(env: Env, request: Request, pirateId: string) {
  const data = await body(request);
  const logs = Array.isArray(data.logs) ? data.logs.map(cleanLog) : [];
  if (logs.length > SYNC_MAX_LOGS) throw new HttpError(413, `${SYNC_MAX_LOGS} lignes maximum par envoi`);
  const now = nowIso();

  const statements = logs.map((log) =>
    env.DB.prepare(
      `INSERT INTO exploits (id, pirate_id, entry, at, note, synced_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING`,
    ).bind(log.id, pirateId, log.entry, log.at, log.note, now),
  );

  const chest = data.chest as Record<string, unknown> | undefined;
  if (chest !== undefined) {
    const doubloons = Number(chest?.doubloons);
    const voyages = Number(chest?.voyages);
    if (![doubloons, voyages].every((n) => Number.isInteger(n) && n >= 0)) throw new HttpError(400, "Coffre invalide");
    // Le butin ne fait que grossir : on garde le plus gros des deux coffres.
    statements.push(
      env.DB.prepare(
        `INSERT INTO chests (pirate_id, doubloons, voyages, updated_at) VALUES (?, ?, ?, ?)
         ON CONFLICT(pirate_id) DO UPDATE SET doubloons = MAX(chests.doubloons, excluded.doubloons),
           voyages = MAX(chests.voyages, excluded.voyages), updated_at = excluded.updated_at`,
      ).bind(pirateId, doubloons, voyages, now),
    );
  }
  if (statements.length > 0) await env.DB.batch(statements);

  const [stored, coffre] = await env.DB.batch([
    env.DB.prepare("SELECT id, entry, at, note FROM exploits WHERE pirate_id = ? ORDER BY at DESC").bind(pirateId),
    env.DB.prepare("SELECT doubloons, voyages FROM chests WHERE pirate_id = ?").bind(pirateId),
  ]);
  const chestRow = (coffre.results[0] as { doubloons: number; voyages: number } | undefined) ?? { doubloons: 0, voyages: 0 };
  return json({
    logs: stored.results,
    chest: chestRow,
  });
}

// --- L'équipage ----------------------------------------------------------------

/** Les pirates publics et leur journal du dernier mois, pour l'écran des statistiques (accessible sans compte). */
async function crew(env: Env) {
  const since = new Date(Date.now() - CREW_DAYS * 86_400_000).toISOString().slice(0, 10);
  const { results } = await env.DB.prepare(
    `SELECT p.id, p.name, l.entry, l.at, l.note FROM exploits l JOIN pirates p ON p.id = l.pirate_id
     WHERE p.public = 1 AND l.at >= ? ORDER BY l.at DESC LIMIT 5000`,
  )
    .bind(since)
    .all<{ id: string; name: string; entry: string; at: string; note: string | null }>();

  // Regroupé par pirate, sans exposer son id.
  const byPirate = new Map<string, { name: string; logs: { entry: string; at: string; note: string | null }[] }>();
  for (const row of results) {
    const mate = byPirate.get(row.id) ?? { name: row.name, logs: [] };
    mate.logs.push({ entry: row.entry, at: row.at, note: row.note });
    byPirate.set(row.id, mate);
  }
  return json({ crew: [...byPirate.values()] });
}

// --- Aiguillage ------------------------------------------------------------------

async function route(request: Request, env: Env): Promise<Response> {
  const { pathname } = new URL(request.url);
  const key = `${request.method} ${pathname}`;
  switch (key) {
    case "GET /":
      return json({ ahoy: "L'API du capitaine est à flot" });
    case "POST /auth/google":
      return googleLogin(env, request);
    case "POST /auth/email/start":
      return emailStart(env, request);
    case "POST /auth/email/verify":
      return emailVerify(env, request);
    case "POST /auth/register":
      return register(env, request);
    case "POST /auth/login":
      return passwordLogin(env, request);
    case "POST /auth/recover":
      return recover(env, request);
    case "POST /me/recovery":
      return newRecoveryCode(env, await pirateOf(env, request));
    case "POST /me/google":
      return linkGoogle(env, request, await pirateOf(env, request));
    case "DELETE /me/google":
      return unlinkGoogle(env, await pirateOf(env, request));
    case "POST /me/password":
      return changePassword(env, request, await pirateOf(env, request));
    case "POST /auth/logout":
      return logout(env, request);
    case "GET /me":
      return json(await me(env, await pirateOf(env, request)));
    case "PATCH /me":
    case "POST /me": // HttpURLConnection (Android) ne sait pas envoyer de PATCH
      return updateMe(env, request, await pirateOf(env, request));
    case "DELETE /me":
      return deleteMe(env, await pirateOf(env, request));
    case "POST /me/head":
      return chooseHead(env, request, await pirateOf(env, request));
    case "POST /sync":
      return sync(env, request, await pirateOf(env, request));
    case "GET /crew":
      return crew(env);
    default:
      throw new HttpError(404, "Rien à piller ici");
  }
}

export default {
  async fetch(request, env): Promise<Response> {
    try {
      return await route(request, env);
    } catch (error) {
      if (error instanceof HttpError) return json({ error: error.message }, error.status);
      console.error(error);
      return json({ error: "Le navire a pris l'eau" }, 500);
    }
  },
} satisfies ExportedHandler<Env>;
