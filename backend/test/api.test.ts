import { SELF } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { base64Url } from "../src/crypto";
import { forgetGoogleKeys } from "../src/google";

const realFetch = globalThis.fetch;
let sentCodes: string[] = [];
let brevoMails: any[] = [];
let googleKeys: { kid: string; jwk: JsonWebKey; privateKey: CryptoKey } | null = null;

beforeEach(() => {
  sentCodes = [];
  brevoMails = [];
  forgetGoogleKeys();
  // Les appels sortants (Brevo, Resend, clés Google) sont interceptés ; les appels à l'API passent.
  vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
    const url = input instanceof Request ? input.url : String(input);
    if (url === "https://api.resend.com/emails") {
      const payload = JSON.parse(String(init?.body));
      sentCodes.push(/(\d{6})/.exec(payload.subject)![1]);
      return Response.json({ id: "email-1" });
    }
    if (url === "https://api.brevo.com/v3/smtp/email") {
      const payload = JSON.parse(String(init?.body));
      brevoMails.push({ payload, apiKey: new Headers(init?.headers).get("api-key") });
      sentCodes.push(/(\d{6})/.exec(payload.subject)![1]);
      return Response.json({ messageId: "brevo-1" }, { status: 201 });
    }
    if (url === "https://www.googleapis.com/oauth2/v3/certs") {
      return Response.json({ keys: googleKeys ? [{ ...googleKeys.jwk, kid: googleKeys.kid, alg: "RS256", use: "sig" }] : [] });
    }
    return realFetch(input, init);
  });
  env.RESEND_API_KEY = "re_test";
});

afterEach(() => {
  vi.restoreAllMocks();
  delete env.BREVO_API_KEY;
});

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
    expect(me.account).toEqual({ kind: "google", email: "mary@read.fr", login: null, google: true });
  });

  it("garde le pseudo envoyé à la création (celui du matelot local)", async () => {
    const response = await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-pseudo" }), name: "Calico Jack" } });
    expect((await response.json<any>()).me.name).toBe("Calico Jack");
  });
});

describe("lier Google à un compte existant", () => {
  const register = async (login: string) =>
    (await api("/auth/register", { json: { login, password: "motdepasse-solide", name: "Grace O'Malley" } })).json<any>();

  it("lie Google au pirate connecté, puis la connexion Google retombe sur lui", async () => {
    const { token, me } = await register("grace@flotte.fr");
    expect(me.account.google).toBe(false);
    const linked = await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-grace", email: "grace@gmail.com" }) } });
    expect(linked.status).toBe(200);
    expect((await linked.json<any>()).account).toMatchObject({ kind: "password", login: "grace@flotte.fr", google: true });

    const viaGoogle = await (await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-grace" }) } })).json<any>();
    expect(viaGoogle.created).toBe(false);
    expect(viaGoogle.me).toMatchObject({ name: "Grace O'Malley", account: { kind: "password", login: "grace@flotte.fr" } });

    // Relier le même Google au même pirate : sans effet, sans erreur.
    expect((await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-grace" }) } })).status).toBe(200);
  });

  it("refuse un Google déjà lié à un autre pirate, sans rien effacer", async () => {
    await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-pris" }), name: "Premier Occupant" } });
    const { token } = await register("second@flotte.fr");
    const refused = await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-pris" }) } });
    expect(refused.status).toBe(409);
    const owner = await (await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-pris" }) } })).json<any>();
    expect(owner.me.name).toBe("Premier Occupant");
  });

  it("refuse un second Google, et un jeton Google invalide", async () => {
    const { token } = await register("deux@flotte.fr");
    expect((await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-un" }) } })).status).toBe(200);
    expect((await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-deux" }) } })).status).toBe(409);
    expect((await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-x", aud: "autre-appli" }) } })).status).toBe(401);
    expect((await api("/me/google", { json: { idToken: "x" } })).status).toBe(401);
  });

  it("délie Google s'il reste le mot de passe, jamais pour un pirate 100 % Google", async () => {
    const { token } = await register("delie@flotte.fr");
    await api("/me/google", { token, json: { idToken: await googleToken({ sub: "g-delie" }) } });
    const unlinked = await api("/me/google", { method: "DELETE", token });
    expect((await unlinked.json<any>()).account.google).toBe(false);
    const fresh = await (await api("/auth/google", { json: { idToken: await googleToken({ sub: "g-delie" }) } })).json<any>();
    expect(fresh.created).toBe(true);

    expect((await api("/me/google", { method: "DELETE", token: fresh.token })).status).toBe(400);
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

describe("identifiant, mot de passe et code secret", () => {
  const register = (login: string, password = "motdepasse-solide", name?: string) =>
    api("/auth/register", { json: { login, password, name } });

  it("crée un compte, montre le code secret une fois, et l'identifiant reste privé", async () => {
    const response = await register("Anne.Bonny@Flotte.fr", "rhum-et-voiles", "Anne la Terrible");
    expect(response.status).toBe(200);
    const created = await response.json<any>();
    expect(created.token).toBeTruthy();
    expect(created.recoveryCode).toMatch(/^[0-9A-Z]{4}(-[0-9A-Z]{4}){4}$/);
    expect(created.me).toMatchObject({ name: "Anne la Terrible", account: { kind: "password", login: "anne.bonny@flotte.fr" } });

    const { crew } = await (await api("/crew")).json<any>();
    expect(JSON.stringify(crew)).not.toContain("anne.bonny@flotte.fr");
  });

  it("refuse un identifiant déjà pris (sans tenir compte des majuscules) et un mot de passe trop court", async () => {
    expect((await register("kraken@flotte.fr")).status).toBe(200);
    expect((await register("KRAKEN@FLOTTE.FR")).status).toBe(409);
    expect((await register("petit@flotte.fr", "court")).status).toBe(400);
    expect((await register("x")).status).toBe(400);
  });

  it("se connecte avec le bon mot de passe, sans dire ce qui cloche sinon", async () => {
    await register("jack.sparrow@flotte.fr", "black-pearl-1");
    const ok = await api("/auth/login", { json: { login: "Jack.Sparrow@flotte.fr", password: "black-pearl-1" } });
    expect(ok.status).toBe(200);
    expect((await ok.json<any>()).me.account.login).toBe("jack.sparrow@flotte.fr");

    const badPassword = await api("/auth/login", { json: { login: "jack.sparrow@flotte.fr", password: "mauvais-mdp" } });
    const unknownLogin = await api("/auth/login", { json: { login: "personne@flotte.fr", password: "black-pearl-1" } });
    expect(badPassword.status).toBe(401);
    expect(unknownLogin.status).toBe(401);
    expect((await badPassword.json<any>()).error).toBe((await unknownLogin.json<any>()).error);
  });

  it("après 5 essais ratés, verrouille 2 minutes (en le disant), puis une minute de plus par échec", async () => {
    const login = "calico.jack@flotte.fr";
    await register(login, "bon-mot-de-passe");
    for (let i = 0; i < 5; i++) {
      expect((await api("/auth/login", { json: { login, password: `faux-${i}-xxxx` } })).status).toBe(401);
    }
    const locked = await api("/auth/login", { json: { login, password: "bon-mot-de-passe" } });
    expect(locked.status).toBe(429);
    expect((await locked.json<any>()).error).toContain("2 minutes");

    // 3 minutes plus tard, la cale est rouverte.
    const past = new Date(Date.now() - 3 * 60_000).toISOString();
    await env.DB.prepare("UPDATE auth_failures SET created_at = ? WHERE login = ?").bind(past, login).run();
    expect((await api("/auth/login", { json: { login, password: "bon-mot-de-passe" } })).status).toBe(200);

    // Un 6e échec : verrou de 3 minutes.
    expect((await api("/auth/login", { json: { login, password: "encore-faux-1" } })).status).toBe(401);
    const longer = await api("/auth/login", { json: { login, password: "bon-mot-de-passe" } });
    expect((await longer.json<any>()).error).toContain("3 minutes");
  });

  it("mot de passe oublié : le code secret remet un mot de passe et en donne un nouveau", async () => {
    const { recoveryCode } = await (await register("mary.read@flotte.fr", "ancien-mdp-123")).json<any>();
    const typed = recoveryCode.toLowerCase().replace(/-/g, " ");
    const recovered = await api("/auth/recover", { json: { login: "mary.read@flotte.fr", code: typed, newPassword: "nouveau-mdp-456" } });
    expect(recovered.status).toBe(200);
    const { recoveryCode: fresh, token } = await recovered.json<any>();
    expect(token).toBeTruthy();
    expect(fresh).not.toBe(recoveryCode);

    expect((await api("/auth/login", { json: { login: "mary.read@flotte.fr", password: "ancien-mdp-123" } })).status).toBe(401);
    expect((await api("/auth/login", { json: { login: "mary.read@flotte.fr", password: "nouveau-mdp-456" } })).status).toBe(200);
    // L'ancien code a servi : il ne marche plus.
    expect((await api("/auth/recover", { json: { login: "mary.read@flotte.fr", code: recoveryCode, newPassword: "encore-un-mdp" } })).status).toBe(401);
  });

  it("donne un nouveau code secret et change le mot de passe une fois connecté", async () => {
    const first = await (await register("rackham@flotte.fr", "premier-mdp-1")).json<any>();
    const renewed = await api("/me/recovery", { method: "POST", token: first.token });
    expect(renewed.status).toBe(200);
    const { recoveryCode } = await renewed.json<any>();
    expect(recoveryCode).not.toBe(first.recoveryCode);
    expect((await api("/auth/recover", { json: { login: "rackham@flotte.fr", code: first.recoveryCode, newPassword: "zzzzzzzz" } })).status).toBe(401);

    expect((await api("/me/password", { token: first.token, json: { current: "faux-mdp-xx", new: "second-mdp-2" } })).status).toBe(401);
    expect((await api("/me/password", { token: first.token, json: { current: "premier-mdp-1", new: "second-mdp-2" } })).status).toBe(200);
    expect((await api("/auth/login", { json: { login: "rackham@flotte.fr", password: "second-mdp-2" } })).status).toBe(200);
  });

  it("supprimer le compte efface l'identifiant : il ne permet plus de se connecter", async () => {
    const { token } = await (await register("ching.shih@flotte.fr", "flotte-rouge-1")).json<any>();
    expect((await api("/me", { method: "DELETE", token })).status).toBe(200);
    expect((await api("/auth/login", { json: { login: "ching.shih@flotte.fr", password: "flotte-rouge-1" } })).status).toBe(401);
    expect((await register("ching.shih@flotte.fr")).status).toBe(200);
  });
});

describe("niveaux et têtes du pirate", () => {
  const sailor = async (login: string, doubloons: number) => {
    const { token } = await (await api("/auth/register", { json: { login, password: "tete-de-mort", name: "Tête Brûlée" } })).json<any>();
    await api("/sync", { token, json: { logs: [], chest: { doubloons, voyages: 1 } } });
    return token as string;
  };
  const chooseHead = (token: string, head: unknown) => api("/me/head", { token, json: { head } });

  it("donne le niveau tiré des doublons, avec la tête de départ", async () => {
    const token = await sailor("novice@tetes.fr", 149);
    expect(await (await api("/me", { token })).json<any>()).toMatchObject({ level: 2, head: 0, heads: [] });
  });

  it("refuse de changer de tête avant le niveau 5, sauf pour la tête de départ", async () => {
    const token = await sailor("mousse@tetes.fr", 499);
    expect((await chooseHead(token, 7)).status).toBe(403);
    // Les têtes de départ : garçon, fille, je ne dis rien.
    for (const head of [0, 50, 100]) expect((await chooseHead(token, head)).status).toBe(200);
    expect(await (await api("/me", { token })).json<any>()).toMatchObject({ head: 100, heads: [] });
  });

  it("au niveau 5, 3 têtes au choix, puis on navigue librement entre elles", async () => {
    const token = await sailor("bosco@tetes.fr", 500);
    for (const head of [3, 9, 12]) expect((await chooseHead(token, head)).status).toBe(200);
    const refused = await chooseHead(token, 20);
    expect(refused.status).toBe(403);
    const back = await (await chooseHead(token, 9)).json<any>();
    expect(back).toMatchObject({ level: 5, head: 9, heads: [3, 9, 12] });
    expect((await chooseHead(token, 0)).status).toBe(200);
  });

  it("au niveau 10, toutes les têtes, sans les compter", async () => {
    const token = await sailor("capitaine@tetes.fr", 2250);
    for (const head of [1, 2, 3, 4, 149]) expect((await chooseHead(token, head)).status).toBe(200);
    expect(await (await api("/me", { token })).json<any>()).toMatchObject({ level: 10, head: 149, heads: [] });
  });

  it("refuse une tête qui n'existe pas", async () => {
    const token = await sailor("perdu@tetes.fr", 5000);
    for (const head of [-1, 196, 2.5, "3"]) expect((await chooseHead(token, head)).status).toBe(400);
    expect((await chooseHead(token, 195)).status).toBe(200);
  });
});

describe("envoi des codes", () => {
  it("passe par Brevo quand sa clé est là, avec l'expéditeur découpé en nom et adresse", async () => {
    env.BREVO_API_KEY = "xkeysib-test";
    const { created } = await emailLogin("brevo@flotte.fr", "Barbe Bleue");
    expect(created).toBe(true);
    expect(brevoMails).toHaveLength(1);
    const { payload, apiKey } = brevoMails[0];
    expect(apiKey).toBe("xkeysib-test");
    expect(payload.to).toEqual([{ email: "brevo@flotte.fr" }]);
    expect(payload.sender.email).toMatch(/@/);
    expect(payload.sender.email).not.toContain("<");
    expect(payload.textContent).toContain(sentCodes.at(-1));
  });
});
