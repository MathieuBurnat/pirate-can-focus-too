package dev.mathieuburnat.piratefocus.player

/**
 * Le niveau du pirate, tiré des doublons du coffre (logique pure, testée ; même calcul que backend/src/levels.ts).
 * Niveau 5 : 3 têtes au choix parmi les têtes prédéfinies. Niveau 10 : toutes les têtes.
 */
object PirateLevel {

    const val HEADS_LEVEL = 5
    const val HEADS_AT_LEVEL_5 = 3
    const val ALL_HEADS_LEVEL = 10

    /** Doublons nécessaires pour atteindre [level] : 0, 50, 150, 300, 500... */
    fun doubloonsFor(level: Int): Int = 25 * (level - 1) * level

    fun of(doubloons: Int): Int {
        var level = 1
        while (doubloons >= doubloonsFor(level + 1)) level++
        return level
    }

    /** Avancement vers le niveau suivant, de 0f à 1f. */
    fun progress(doubloons: Int): Float {
        val level = of(doubloons)
        val from = doubloonsFor(level)
        return (doubloons - from).toFloat() / (doubloonsFor(level + 1) - from)
    }

    /** Têtes que l'on peut débloquer à ce niveau (Int.MAX_VALUE au niveau 10 : toutes). */
    fun headSlots(level: Int): Int = when {
        level >= ALL_HEADS_LEVEL -> Int.MAX_VALUE
        level >= HEADS_LEVEL -> HEADS_AT_LEVEL_5
        else -> 0
    }

    /** Peut-on porter [head] tout de suite (tête de départ, déjà débloquée, ou niveau 10) ? */
    fun canWear(head: Int, unlocked: List<Int>, level: Int): Boolean =
        head == 0 || head in unlocked || level >= ALL_HEADS_LEVEL

    /** Places encore libres pour débloquer une tête (0 avant le niveau 5, ou une fois les 3 choisies). */
    fun freeSlots(unlocked: List<Int>, level: Int): Int = (headSlots(level) - unlocked.size).coerceAtLeast(0)

    /**
     * Choisir [head] : la liste des têtes débloquées qui en découle, ou null si c'est refusé
     * (avant le niveau 5, ou plus de place). Au niveau 10, on ne compte plus.
     */
    fun choose(head: Int, unlocked: List<Int>, level: Int): List<Int>? = when {
        canWear(head, unlocked, level) -> unlocked
        freeSlots(unlocked, level) > 0 -> unlocked + head
        else -> null
    }
}
