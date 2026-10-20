// Les bindings du Worker, vus depuis les tests (`import { env } from "cloudflare:workers"`).
declare namespace Cloudflare {
  interface Env {
    DB: D1Database;
    GOOGLE_CLIENT_IDS: string;
    EMAIL_FROM: string;
    BREVO_API_KEY?: string;
    RESEND_API_KEY?: string;
    DEV_LOG_CODES?: string;
    TEST_MIGRATIONS: import("cloudflare:test").D1Migration[];
  }
}
