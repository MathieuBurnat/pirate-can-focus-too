export interface Env {
  DB: D1Database;
  /** Identifiants OAuth Google acceptés, séparés par des virgules. */
  GOOGLE_CLIENT_IDS: string;
  EMAIL_FROM: string;
  RESEND_API_KEY?: string;
  /** "1" : sans clé Resend, les codes sont affichés dans la console (développement seulement). */
  DEV_LOG_CODES?: string;
}
