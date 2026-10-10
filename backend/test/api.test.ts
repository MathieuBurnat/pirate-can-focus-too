import { SELF } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { base64Url } from "../src/crypto";
import { forgetGoogleKeys } from "../src/google";

const realFetch = globalThis.fetch;
let sentCodes: string[] = [];
let googleKeys: { kid: string; jwk: JsonWebKey; privateKey: CryptoKey } | null = null;

beforeEach(() => {
  sentCodes = [];
  forgetGoogleKeys();
  // Les appels sortants (Resend, clés Google) sont interceptés ; les appels à l'API passent.
  vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const url = input instanceof Request ? input.url : String(input);
    if (url === "https://api.resend.com/emails") {
      const payload = JSON.parse(String(init?.body));
      sentCodes.push(/(\d{6})/.exec(payload.subject)![1]);
      return Response.json({ id: "email-1" });
    }
    if (url === "https://www.googleapis.com/oauth2/v3/certs") {
      return Response.json({ keys: googleKeys ? [{ ...googleKeys.jwk, kid: googleKeys.kid, alg: "RS256", use: "sig" }] : [] });
    }
    return realFetch(input, init);
  });
  env.RESEND_API_KEY = "re_test";
});

afterEach(() => vi.restoreAllMocks());

const api = (path: string, init: RequestInit & { token?: string; json?: unknown } = {}) =>
  SELF.fetch(`https://api.test${path}`, {
    method: init.method ?? (init.json ? "POST" : "GET"),
    headers: {
      // Chaque appel vient d'un appareil différent, pour ne pas buter sur la limite d'envois par IP.
      "CF-Connecting-IP": `198.51.100.${Math.floor(Math.random() * 250)}-${crypto.randomUUID()}`,
      ...(init.json ? { "Content-Type": "application/json" } : {}),
      ...(init.token ? { Authorization: `Bearer ${init.token}` } : {}),
    },
    body: init.json ? JSON.stringify(init.json) : undefined,
  });

async function emailLogin(email: string, name?: string): Promise<{ token: string; created: boolean; me: any }> {
  expect((await api("/auth/email/start", { json: { email } })).status).toBe(200);
  const response = await api("/auth/email/verify", { json: { email, code: sentCodes.at(-1), name } });
  expect(response.status).toBe(200);
  return response.json();
}

async function googleToken(claims: Record<string, unknown>) {
  if (!googleKeys) {
    const pair = (await crypto.subtle.generateKey(
      { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
      true,
      ["sign", "verify"],
    )) as CryptoKeyPair;
    googleKeys = { kid: "cle-1", jwk: (await crypto.subtle.exportKey("jwk", pair.publicKey)) as JsonWebKey, privateKey: pair.privateKey };
  }
  const encode = (data: unknown) => base64Url(new TextEncoder().encode(JSON.stringify(data)));
  const unsigned = `${encode({ alg: "RS256", kid: googleKeys.kid, typ: "JWT" })}.${encode({
    iss: "https://accounts.google.com",
    aud: "client-android.apps.googleusercontent.com",
    exp: Math.floor(Date.now() / 1000) + 3600,
    ...claims,
  })}`;
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", googleKeys.privateKey, new TextEncoder().encode(unsigned));
  return `${unsigned}.${base64Url(new Uint8Array(signature))}`;
}

describe("connexion par code email", () => {
  it("crée un pirate avec le pseudo choisi, puis retrouve le même", async () => {
    const first = await emailLogin("Anne@Bonny.fr", "Anne la Terrible");
    expect(first.created).toBe(true);
    expect(first.me).toMatchObject({ name: "Anne la Terrible", public: true, account: { kind: "email", email: "anne@bonny.fr" } });

    await env.DB.prepare("UPDATE email_codes SET created_at = '2000-01-01T00:00:00Z'").run();
    const again = await emailLogin("anne@bonny.fr");
    expect(again.created).toBe(false);
    expect(again.me.name).toBe("Anne la Terrible");
  });

  it("tire un pseudo de pirate au sort quand on n'en donne pas", async () => {
    const { me } = await emailLogin("jack@sparrow.fr");
    expect(me.name.split(" ").length).toBeGreaterThanOrEqual(3);
  });

  it("refuse un mauvais code, puis tout code après 5 essais", async () => {
    await api("/auth/email/start", { json: { email: "rackham@mer.fr" } });
    for (let i = 0; i < 5; i++) {
      expect((await api("/auth/email/verify", { json: { email: "rackham@mer.fr", code: "000000" === sentCodes[0] ? "111111" : "000000" } })).status).toBe(401);
    }
    const late = await api("/auth/email/verify", { json: { email: "rackham@mer.fr", code: sentCodes[0] } });
    expect(late.status).toBe(401);
  });

  it("ne renvoie pas un code dans la minute", async () => {
    await api("/auth/email/start", { json: { email: "barbe@noire.fr" } });
    expect((await api("/auth/email/start", { json: { email: "barbe@noire.fr" } })).status).toBe(429);
  });

  it("limite les envois par appareil, toutes adresses confondues", async () => {
    const from = (email: string) =>
      SELF.fetch("https://api.test/auth/email/start", {
        method: "POST",
        headers: { "Content-Type": "application/json", "CF-Connecting-IP": "203.0.113.7" },
        body: JSON.stringify({ email }),
      });
    for (let i = 0; i < 10; i++) expect((await from(`victime${i}@mer.fr`)).status).toBe(200);
    expect((await from("victime10@mer.fr")).status).toBe(429);
    expect(sentCodes.length).toBe(10);
  });

  it("refuse une adresse invalide", async () => {
    expect((await api("/auth/email/start", { json: { email: "pas-une-adresse" } })).status).toBe(400);
  });
});

describe("connexion Google", () => {
  it("accepte un jeton signé par Google pour notre application", async () => {
    const response = await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-42", email: "mary@read.fr", email_verified: true }) } });
    expect(response.status).toBe(200);
    const { me, created } = await response.json<any>();
    expect(created).toBe(true);
    expect(me.account).toEqual({ kind: "google", email: "mary@read.fr" });
  });

  it("refuse un jeton destiné à une autre application", async () => {
    const response = await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-1", aud: "autre-appli" }) } });
    expect(response.status).toBe(401);
  });

  it("refuse un jeton expiré ou trafiqué", async () => {
    const expired = await googleToken({ sub: "g-2", exp: 1000 });
    expect((await api("/auth/google", { json: { idToken: expired } })).status).toBe(401);
    const valid = await googleToken({ sub: "g-3" });
    const [h, , s] = valid.split(".");
    const forged = `${h}.${base64Url(new TextEncoder().encode(JSON.stringify({ sub: "g-3", aud: "client-android.apps.googleusercontent.com", iss: "accounts.google.com", exp: 9e9, admin: true })))}.${s}`;
    expect((await api("/auth/google", { json: { idToken: forged } })).status).toBe(401);
  });
});

describe("mon compte", () => {
  it("demande un jeton valide", async () => {
    expect((await api("/me")).status).toBe(401);
    expect((await api("/me", { token: "faux" })).status).toBe(401);
  });

  it("change le pseudo et la visibilité publique", async () => {
    const { token } = await emailLogin("cuistot@navire.fr");
    const response = await api("/me", { method: "PATCH", token, json: { name: "Cuistot Gaston", public: false } });
    expect(await response.json()).toMatchObject({ name: "Cuistot Gaston", public: false });
    expect((await api("/me", { method: "PATCH", token, json: { name: "x" } })).status).toBe(400);
    // L'app Android passe par POST (HttpURLConnection ne sait pas faire PATCH).
    const viaPost = await api("/me", { method: "POST", token, json: { public: true } });
    expect(await viaPost.json()).toMatchObject({ name: "Cuistot Gaston", public: true });
  });

  it("la suppression efface l'identité et les petits mots, mais garde l'ancien matelot au classement", async () => {
    const { token } = await emailLogin("adieu@navire.fr", "Hector le Borgne");
    await api("/sync", { token, json: { logs: [{ id: "l1", entry: "BIERE", at: new Date().toISOString(), note: "chez Marcel" }] } });

    expect((await api("/me", { method: "DELETE", token })).status).toBe(200);
    expect((await api("/me", { token })).status).toBe(401);
    expect(await env.DB.prepare("SELECT COUNT(*) AS n FROM identities WHERE email = 'adieu@navire.fr'").first("n")).toBe(0);

    const { crew } = await (await api("/crew")).json<any>();
    const former = crew.find((mate: any) => mate.logs.some((log: any) => log.entry === "BIERE"));
    expect(former.name).toMatch(/^(Ancien matelot|Ancien pirate|Vieux loup de mer) n°\d{3}$/);
    expect(former.logs[0].note).toBeNull();
  });

  it("la déconnexion révoque le jeton", async () => {
    const { token } = await emailLogin("bye@navire.fr");
    await api("/auth/logout", { json: {}, token });
    expect((await api("/me", { token })).status).toBe(401);
  });
});

describe("synchronisation", () => {
  it("garde les lignes pour toujours et ne fait que grossir le coffre", async () => {
    const { token } = await emailLogin("sync@navire.fr");
    const at = "2026-10-10T21:30:15.123";
    let response = await api("/sync", {
      token,
      json: {
        logs: [
          { id: "a", entry: "GRIMPE", at, note: "bloc 6a" },
          { id: "b", entry: "VIN", at },
        ],
        chest: { doubloons: 120, voyages: 4 },
      },
    });
    expect(response.status).toBe(200);

    // Réécrire une ligne déjà notée ne change rien : ce qui est écrit est écrit.
    response = await api("/sync", { token, json: { logs: [{ id: "b", entry: "ABDOS", at }], chest: { doubloons: 30, voyages: 1 } } });
    const data = await response.json<any>();
    expect(data.chest).toEqual({ doubloons: 120, voyages: 4 });
    expect(data.logs).toHaveLength(2);
    expect(data.logs.find((l: any) => l.id === "b").entry).toBe("VIN");
    expect(data.logs.find((l: any) => l.id === "a")).toMatchObject({ entry: "GRIMPE", note: "bloc 6a" });
  });

  it("n'écrase jamais la ligne d'un autre pirate", async () => {
    const anne = await emailLogin("anne@flotte.fr");
    const jack = await emailLogin("jack@flotte.fr");
    const at = new Date().toISOString();
    await api("/sync", { token: anne.token, json: { logs: [{ id: "commun", entry: "ABDOS", at }] } });
    await api("/sync", { token: jack.token, json: { logs: [{ id: "commun", entry: "BIERE", at }] } });
    const row = await env.DB.prepare("SELECT entry FROM exploits WHERE id = 'commun'").first("entry");
    expect(row).toBe("ABDOS");
  });

  it("refuse une entrée inconnue", async () => {
    const { token } = await emailLogin("kraken@navire.fr");
    const response = await api("/sync", { token, json: { logs: [{ id: "k", entry: "KRAKEN", at: "2026-10-10T10:00" }] } });
    expect(response.status).toBe(400);
  });
});

describe("équipage", () => {
  it("montre les pirates publics à tout le monde, sans compte, et cache les autres", async () => {
    const visible = await emailLogin("visible@flotte.fr", "Morgane Pied-Marin");
    const hidden = await emailLogin("cache@flotte.fr", "Octave Discret");
    await api("/me", { method: "PATCH", token: hidden.token, json: { public: false } });
    const at = new Date().toISOString();
    await api("/sync", { token: visible.token, json: { logs: [{ id: "v1", entry: "MEGA_SEANCE", at }] } });
    await api("/sync", { token: hidden.token, json: { logs: [{ id: "h1", entry: "MEGA_SEANCE", at }] } });

    const { crew } = await (await api("/crew")).json<any>();
    const names = crew.map((mate: any) => mate.name);
    expect(names).toContain("Morgane Pied-Marin");
    expect(names).not.toContain("Octave Discret");
    expect(JSON.stringify(crew)).not.toContain("visible@flotte.fr");
  });
});
