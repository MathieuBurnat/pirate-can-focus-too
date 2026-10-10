package dev.mathieuburnat.piratefocus.journal

import kotlin.random.Random

enum class Period(val label: String, val days: Int) {
    JOUR("JOUR", 1),
    SEMAINE("SEMAINE", 7),
    MOIS("MOIS", 30),
}

/** Les points d'un matelot sur une période. */
data class Score(val biscotos: Int, val taverne: Int) {
    val verdict: Verdict
        get() = JournalState(
            mapOf(Entry.MEGA_SEANCE to biscotos, Entry.BIERE to taverne),
        ).verdict

    operator fun plus(other: Score) = Score(biscotos + other.biscotos, taverne + other.taverne)
}

data class CrewMate(val name: String, val days: List<Score>) {
    /** Les jours sont rangés du plus récent au plus ancien. */
    fun score(period: Period): Score = days.take(period.days).fold(Score(0, 0), Score::plus)
}

/** Données de démonstration [dev] : un équipage imaginaire, en attendant les vrais matelots. */
object FakeCrew {

    private val firstNames = listOf(
        "Jack", "Mary", "Bill", "Anne", "Barnabé", "Gertrude", "Hector", "Lulu",
        "Edouard", "Rosalie", "Gaston", "Margot", "Firmin", "Ursule", "Octave", "Pépita",
    )
    private val nicknames = listOf(
        "la Sardine", "Grand-Bras", "Coude-Léger", "Barbe-Molle", "l'Éponge", "Jambe-de-Bois",
        "Mille-Pompes", "Tonneau", "la Vigie", "Crochet-Rouillé", "le Kraken", "Sans-Soif",
        "Gosier-Sec", "la Mouette", "Biscotos", "Cale-Humide",
    )

    /** Chaque matelot a un tempérament : plutôt salle de sport, plutôt taverne, ou entre les deux. */
    private enum class Temper(val sportChance: Double, val drinkChance: Double) {
        ATHLETE(0.75, 0.15),
        POCHARD(0.15, 0.75),
        EQUILIBRE(0.45, 0.45),
        FLEMMARD(0.10, 0.20),
    }

    fun generate(size: Int = 8, random: Random = Random.Default): List<CrewMate> {
        val names = firstNames.shuffled(random).zip(nicknames.shuffled(random)) { first, nick -> "$first $nick" }
        return names.take(size).map { name ->
            val temper = Temper.entries.random(random)
            val days = List(Period.MOIS.days) {
                Score(
                    biscotos = if (random.nextDouble() < temper.sportChance) random.nextInt(1, 4) else 0,
                    taverne = if (random.nextDouble() < temper.drinkChance) random.nextInt(1, 5) else 0,
                )
            }
            CrewMate(name, days)
        }
    }
}

/** Le petit nom que donne le capitaine selon le verdict. */
fun Verdict.nickname(): String = when (this) {
    Verdict.PAGE_BLANCHE -> "fantôme"
    Verdict.ATHLETE -> "moine-soldat"
    Verdict.EPONGE -> "éponge de cale"
    Verdict.PILIER_DE_TAVERNE -> "pilier de taverne"
    Verdict.EQUILIBRE -> "vrai pirate"
    Verdict.SPORTIF -> "costaud"
}
