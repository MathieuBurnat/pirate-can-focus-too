/** Les pseudos de pirate tirés au sort, et le nom laissé par un compte supprimé. */

const TITLES = ["Capitaine", "Matelot", "Moussaillon", "Quartier-maître", "Boucanier", "Corsaire", "Flibustier", "Cuistot"];
const NAMES = ["Jack", "Anne", "Barnabé", "Gaston", "Rosalie", "Hector", "Morgane", "Fernand", "Lucette", "Octave", "Mireille", "Bertrand"];
const NICKNAMES = [
  "le Borgne",
  "Jambe-de-Bois",
  "Barbe-Rousse",
  "Sans-Dents",
  "la Sardine",
  "la Tempête",
  "Crochet-Rouillé",
  "Mal-Rasé",
  "Pied-Marin",
  "Rhum-Arrangé",
  "Coque-Percée",
  "Mange-Bigorneaux",
];

const FORMER = ["Ancien matelot", "Ancien pirate", "Vieux loup de mer"];

function pick<T>(list: readonly T[]): T {
  return list[crypto.getRandomValues(new Uint32Array(1))[0] % list.length];
}

export function randomPirateName(): string {
  return `${pick(TITLES)} ${pick(NAMES)} ${pick(NICKNAMES)}`;
}

/** Ce qu'il reste d'un pirate dont le compte a été supprimé. */
export function formerPirateName(): string {
  const number = crypto.getRandomValues(new Uint16Array(1))[0] % 1000;
  return `${pick(FORMER)} n°${String(number).padStart(3, "0")}`;
}

/** Un pseudo choisi : 2 à 48 caractères, espaces superflus retirés. Null s'il ne convient pas. */
export function cleanPirateName(name: unknown): string | null {
  if (typeof name !== "string") return null;
  const cleaned = name.replace(/\s+/g, " ").trim();
  return cleaned.length >= 2 && cleaned.length <= 48 ? cleaned : null;
}
