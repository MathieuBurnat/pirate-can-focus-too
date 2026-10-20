import { fromBase64Url } from "./crypto";

/** Ce qu'on garde d'un jeton Google vérifié. */
export interface GoogleUser {
  sub: string;
  email?: string;
}

interface Jwk extends JsonWebKey {
  kid: string;
}

const JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
const ISSUERS = ["accounts.google.com", "https://accounts.google.com"];

let cachedKeys: { keys: Jwk[]; until: number } | null = null;

async function googleKeys(): Promise<Jwk[]> {
  if (cachedKeys && cachedKeys.until > Date.now()) return cachedKeys.keys;
  const response = await fetch(JWKS_URL);
  if (!response.ok) throw new Error(`Clés Google introuvables (${response.status})`);
  const { keys } = (await response.json()) as { keys: Jwk[] };
  const maxAge = Number(/max-age=(\d+)/.exec(response.headers.get("cache-control") ?? "")?.[1] ?? 3600);
  cachedKeys = { keys, until: Date.now() + maxAge * 1000 };
  return keys;
}

/** Pour les tests : oublie les clés gardées en mémoire. */
export function forgetGoogleKeys() {
  cachedKeys = null;
}

/**
 * Vérifie un ID token Google (signature RS256, émetteur, audience, expiration).
 * Renvoie null si le jeton ne convient pas.
 */
export async function verifyGoogleIdToken(token: string, clientIds: string[], now = Date.now()): Promise<GoogleUser | null> {
  const parts = token.split(".");
  if (parts.length !== 3 || clientIds.length === 0) return null;
  try {
    const decode = (part: string) => JSON.parse(new TextDecoder().decode(fromBase64Url(part)));
    const header = decode(parts[0]);
    const payload = decode(parts[1]);
    if (header.alg !== "RS256") return null;

    const jwk = (await googleKeys()).find((key) => key.kid === header.kid);
    if (!jwk) return null;
    const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
    const signed = new TextEncoder().encode(`${parts[0]}.${parts[1]}`);
    if (!(await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, fromBase64Url(parts[2]), signed))) return null;

    if (!ISSUERS.includes(payload.iss)) return null;
    if (!clientIds.includes(payload.aud)) return null;
    if (typeof payload.exp !== "number" || payload.exp * 1000 < now) return null;
    if (typeof payload.sub !== "string") return null;
    return { sub: payload.sub, email: payload.email_verified ? payload.email : undefined };
  } catch {
    return null;
  }
}
