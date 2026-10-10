import { randomCode, randomToken, sameText, sha256 } from "./crypto";
import { sendLoginCode } from "./email";
import type { Env } from "./env";
import { verifyGoogleIdToken } from "./google";
import { cleanPirateName, formerPirateName, randomPirateName } from "./names";

/** Les lignes que le journal du téléphone sait écrire (enum Entry côté app). */
const ENTRIES = new Set(["MEGA_SEANCE", "GRIMPE", "ABDOS", "BIERE", "COCKTAIL", "VIN", "TRAVERSEE"]);

const SESSION_DAYS = 180;
const CODE_MINUTES = 10;
const CODE_COOLDOWN_SECONDS = 60;
const CODE_MAX_ATTEMPTS = 5;
/** Codes envoyés au plus par appareil (IP) et par heure, toutes adresses confondues. */
const CODES_PER_IP_PER_HOUR = 10;
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

// --- Mon compte --------------------------------------------------------------

async function me(env: Env, pirateId: string) {
  const row = await env.DB.prepare(
    `SELECT p.name, p.public, i.email, i.google_sub FROM pirates p LEFT JOIN identities i ON i.pirate_id = p.id WHERE p.id = ?`,
  )
    .bind(pirateId)
    .first<{ name: string; public: number; email: string | null; google_sub: string | null }>();
  if (!row) throw new HttpError(404, "Pirate introuvable");
  return {
    name: row.name,
    public: row.public === 1,
    account: { kind: row.google_sub ? "google" : "email", email: row.email },
  };
}

async function updateMe(env: Env, request: Request, pirateId: string) {
  const data = await body(request);
  if (data.name !== undefined) {
    const name = cleanPirateName(data.name);
    if (!name) throw new HttpError(400, "Pseudo de 2 à 32 caractères attendu");
    await env.DB.prepare("UPDATE pirates SET name = ? WHERE id = ?").bind(name, pirateId).run();
  }
  if (data.public !== undefined) {
    if (typeof data.public !== "boolean") throw new HttpError(400, "public doit valoir true ou false");
    await env.DB.prepare("UPDATE pirates SET public = ? WHERE id = ?").bind(data.public ? 1 : 0, pirateId).run();
  }
  return json(await me(env, pirateId));
}

/**
 * Suppression du compte : l'identité (email, Google) et les sessions disparaissent, les petits mots
 * du journal sont effacés, et le pirate devient un « Ancien matelot » dont les stats restent au classement.
 */
async function deleteMe(env: Env, pirateId: string) {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM identities WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("DELETE FROM sessions WHERE pirate_id = ?").bind(pirateId),
    env.DB.prepare("UPDATE logs SET note = NULL WHERE pirate_id = ?").bind(pirateId),
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
      `INSERT INTO logs (id, pirate_id, entry, at, note, synced_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING`,
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
    env.DB.prepare("SELECT id, entry, at, note FROM logs WHERE pirate_id = ? ORDER BY at DESC").bind(pirateId),
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
    `SELECT p.id, p.name, l.entry, l.at, l.note FROM logs l JOIN pirates p ON p.id = l.pirate_id
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
    case "POST /auth/logout":
      return logout(env, request);
    case "GET /me":
      return json(await me(env, await pirateOf(env, request)));
    case "PATCH /me":
      return updateMe(env, request, await pirateOf(env, request));
    case "DELETE /me":
      return deleteMe(env, await pirateOf(env, request));
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
