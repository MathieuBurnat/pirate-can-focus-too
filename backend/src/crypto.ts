/** Petits outils de cryptographie : jetons, codes, empreintes. */

export function randomToken(bytes = 32): string {
  const buffer = crypto.getRandomValues(new Uint8Array(bytes));
  return base64Url(buffer);
}

/** Un code à 6 chiffres, sans biais de tirage. */
export function randomCode(): string {
  const limit = 4_294_000_000; // multiple de 1 000 000 juste sous 2^32
  let value: number;
  do {
    value = crypto.getRandomValues(new Uint32Array(1))[0];
  } while (value >= limit);
  return String(value % 1_000_000).padStart(6, "0");
}

/** Alphabet de Crockford : 32 signes, sans I, L, O ni U pour ne pas les confondre. */
const CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

/** Un code de secours : 20 signes (100 bits), en 5 groupes de 4 (« 7QXM-K2PA-... »). */
export function randomRecoveryCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(20));
  const chars = [...bytes].map((b) => CROCKFORD[b & 31]).join("");
  return chars.match(/.{4}/g)!.join("-");
}

/** Ce que le pirate a tapé, ramené à la forme canonique : majuscules, sans tirets, O lu 0, I et L lus 1. */
export function normalizeRecoveryCode(input: string): string {
  return input.toUpperCase().replace(/[^0-9A-Z]/g, "").replace(/O/g, "0").replace(/[IL]/g, "1");
}

/** Itérations PBKDF2 : le maximum accepté par les Workers. */
const PBKDF2_ITERATIONS = 100_000;

/** Empreinte d'un mot de passe (PBKDF2-SHA256, sel aléatoire de 16 octets), en base64url. */
export async function hashPassword(password: string, salt = randomToken(16)): Promise<{ hash: string; salt: string }> {
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", hash: "SHA-256", salt: fromBase64Url(salt), iterations: PBKDF2_ITERATIONS },
    key,
    256,
  );
  return { hash: base64Url(new Uint8Array(bits)), salt };
}

export async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Comparaison à temps constant, pour ne rien laisser deviner d'un code. */
export function sameText(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

export function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function fromBase64Url(text: string): Uint8Array {
  const padded = text.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(text.length / 4) * 4, "=");
  return Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
}
