/**
 * Niveaux et têtes du pirate : même logique que PirateLevel.kt côté app.
 * Le niveau vient des doublons du coffre ; le niveau 5 débloque 3 têtes au choix, le niveau 10 toutes.
 */

/** Nombre de têtes prédéfinies dans l'app (0 = la tête de départ, toujours permise). */
export const HEAD_COUNT = 50;
export const HEADS_LEVEL = 5;
export const HEADS_AT_LEVEL_5 = 3;
export const ALL_HEADS_LEVEL = 10;

/** Doublons nécessaires pour atteindre [level] : 0, 50, 150, 300, 500... */
export const doubloonsFor = (level: number) => 25 * (level - 1) * level;

export function levelOf(doubloons: number): number {
  let level = 1;
  while (doubloons >= doubloonsFor(level + 1)) level++;
  return level;
}

/** Têtes que le pirate peut encore débloquer à ce niveau (Infinity au niveau 10). */
export function headSlots(level: number): number {
  if (level >= ALL_HEADS_LEVEL) return Infinity;
  if (level >= HEADS_LEVEL) return HEADS_AT_LEVEL_5;
  return 0;
}

export const parseHeads = (text: string): number[] =>
  text
    .split(",")
    .map(Number)
    .filter((n) => Number.isInteger(n) && n > 0 && n < HEAD_COUNT);
