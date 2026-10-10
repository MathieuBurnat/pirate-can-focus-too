import type { Env } from "./env";

/** Envoie le code de connexion. Renvoie false si l'envoi a échoué ou n'est pas configuré. */
export async function sendLoginCode(env: Env, to: string, code: string): Promise<boolean> {
  if (!env.RESEND_API_KEY) {
    if (env.DEV_LOG_CODES === "1") {
      console.log(`[dev] code de connexion pour ${to} : ${code}`);
      return true;
    }
    return false;
  }
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${env.RESEND_API_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      from: env.EMAIL_FROM,
      to: [to],
      subject: `${code} : ton code pour monter à bord`,
      text:
        `Ahoy matelot !\n\nTon code pour monter à bord de Pirate Can Focus Too : ${code}\n\n` +
        `Il expire dans 10 minutes. Si tu n'as rien demandé, ignore ce message : personne ne montera sur ton navire.\n\n` +
        `Le Capitaine`,
    }),
  });
  return response.ok;
}
