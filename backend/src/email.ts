import type { Env } from "./env";

/**
 * Envoie le code de connexion, par Brevo si sa clé est là, sinon par Resend.
 * Renvoie false si l'envoi a échoué ou n'est pas configuré.
 */
export async function sendLoginCode(env: Env, to: string, code: string): Promise<boolean> {
  const mail = loginMail(code);
  if (env.BREVO_API_KEY) return sendWithBrevo(env.BREVO_API_KEY, env.EMAIL_FROM, to, mail);
  if (env.RESEND_API_KEY) return sendWithResend(env.RESEND_API_KEY, env.EMAIL_FROM, to, mail);
  if (env.DEV_LOG_CODES === "1") {
    console.log(`[dev] code de connexion pour ${to} : ${code}`);
    return true;
  }
  return false;
}

interface Mail {
  subject: string;
  text: string;
}

function loginMail(code: string): Mail {
  return {
    subject: `${code} : ton code pour monter à bord`,
    text:
      `Ahoy matelot !\n\nTon code pour monter à bord de Pirate Can Focus Too : ${code}\n\n` +
      `Il expire dans 10 minutes. Si tu n'as rien demandé, ignore ce message : personne ne montera sur ton navire.\n\n` +
      `Le Capitaine`,
  };
}

/** « Le Capitaine <capitaine@exemple.ch> » -> nom et adresse (sans chevrons : l'adresse seule). */
export function parseSender(from: string): { name?: string; email: string } {
  const match = /^\s*(.*?)\s*<([^>]+)>\s*$/.exec(from);
  if (!match) return { email: from.trim() };
  return match[1] ? { name: match[1], email: match[2].trim() } : { email: match[2].trim() };
}

async function sendWithBrevo(apiKey: string, from: string, to: string, mail: Mail): Promise<boolean> {
  const response = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: { "api-key": apiKey, "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      sender: parseSender(from),
      to: [{ email: to }],
      subject: mail.subject,
      textContent: mail.text,
    }),
  });
  return response.ok;
}

async function sendWithResend(apiKey: string, from: string, to: string, mail: Mail): Promise<boolean> {
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ from, to: [to], subject: mail.subject, text: mail.text }),
  });
  return response.ok;
}
