package dev.mathieuburnat.piratefocus.journal

enum class Side { SPORT, BOISSON }

enum class Entry(val side: Side, val label: String, val detail: String) {
    MEGA_SEANCE(Side.SPORT, "Méga séance", "pompes, tractions"),
    GRIMPE(Side.SPORT, "Grimpe", "escalade, bloc"),
    ABDOS(Side.SPORT, "P'tite séance abdos", "gainage, crunchs"),
    BIERE(Side.BOISSON, "Bière", "la pinte du marin"),
    COCKTAIL(Side.BOISSON, "Cocktail", "avec une ombrelle"),
    VIN(Side.BOISSON, "Vin !", "rouge, blanc, rosé"),
}

/** Le verdict du capitaine selon le match muscles contre bouteilles. */
enum class Verdict {
    /** Rien de noté. */
    PAGE_BLANCHE,

    /** Que du sport, pas une goutte. */
    ATHLETE,

    /** Que des verres, et au moins trois. */
    EPONGE,

    /** Plus de verres que de séances. */
    PILIER_DE_TAVERNE,

    /** Autant de l'un que de l'autre. */
    EQUILIBRE,

    /** Plus de séances que de verres. */
    SPORTIF,
}

object JournalRules {
    /** Au-delà de ce nombre de verres, le capitaine fait barrage. */
    const val DRINK_LIMIT = 3

    /** Le capitaine refuse un verre de plus au-delà de la limite, sauf si on insiste (second tap). */
    fun refuses(entry: Entry, journal: JournalState, insisting: Boolean): Boolean =
        entry.side == Side.BOISSON && journal.total(Side.BOISSON) >= DRINK_LIMIT && !insisting

    /** À partir de ce nombre de verres dans la soirée, Coco le perroquet débarque. */
    const val PARROT_LIMIT = 8

    /** Coco surgit au 8e verre, puis tous les 4 verres (12, 16...). */
    fun parrotAppears(drinks: Int): Boolean = drinks >= PARROT_LIMIT && (drinks - PARROT_LIMIT) % 4 == 0
}

/** Le carnet de bord du jour (remis à zéro à chaque lancement, pour l'instant). */
data class JournalState(val counts: Map<Entry, Int> = emptyMap()) {

    fun count(entry: Entry): Int = counts[entry] ?: 0

    fun total(side: Side): Int = counts.filterKeys { it.side == side }.values.sum()

    val verdict: Verdict
        get() {
            val sport = total(Side.SPORT)
            val drinks = total(Side.BOISSON)
            return when {
                sport == 0 && drinks == 0 -> Verdict.PAGE_BLANCHE
                drinks == 0 -> Verdict.ATHLETE
                sport == 0 && drinks >= 3 -> Verdict.EPONGE
                drinks > sport -> Verdict.PILIER_DE_TAVERNE
                drinks == sport -> Verdict.EQUILIBRE
                else -> Verdict.SPORTIF
            }
        }

    fun add(entry: Entry): JournalState = copy(counts = counts + (entry to count(entry) + 1))

    fun remove(entry: Entry): JournalState =
        copy(counts = counts + (entry to (count(entry) - 1).coerceAtLeast(0)))
}
