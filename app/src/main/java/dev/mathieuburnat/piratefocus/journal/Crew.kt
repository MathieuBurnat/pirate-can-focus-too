package dev.mathieuburnat.piratefocus.journal

import java.time.LocalDateTime
import kotlin.random.Random

enum class Period(val label: String, val days: Long) {
    JOUR("JOUR", 1),
    SEMAINE("SEMAINE", 7),
    MOIS("MOIS", 30);

    /** La période se termine maintenant et remonte de [days] jours calendaires (aujourd'hui compris). */
    fun contains(at: LocalDateTime, now: LocalDateTime): Boolean =
        !at.isAfter(now) && at.toLocalDate().isAfter(now.toLocalDate().minusDays(days))
}

/** Une ligne du journal de bord : qui a fait (ou bu) quoi, quand, et ce qu'il en dit. */
data class LogEntry(val who: String, val entry: Entry, val at: LocalDateTime, val note: String? = null)

/** Qui afficher dans les statistiques. */
enum class Audience(val label: String) {
    FLOTTE("Toute la flotte"),
    AMIS("Mes amis seulement"),
}

/** Les points d'un matelot sur une période. */
data class Score(val biscotos: Int, val taverne: Int, val traversees: Int = 0) {
    val verdict: Verdict
        get() = JournalState(
            mapOf(Entry.MEGA_SEANCE to biscotos, Entry.BIERE to taverne),
        ).verdict

    operator fun plus(other: Score) =
        Score(biscotos + other.biscotos, taverne + other.taverne, traversees + other.traversees)

    companion object {
        fun of(logs: List<LogEntry>) = Score(
            biscotos = logs.count { it.entry.side == Side.SPORT },
            taverne = logs.count { it.entry.side == Side.BOISSON },
            traversees = logs.count { it.entry.side == Side.FOCUS },
        )
    }
}

data class CrewMate(val name: String, val logs: List<LogEntry>, val friend: Boolean = false) {
    fun score(period: Period, now: LocalDateTime): Score = Score.of(logs.filter { period.contains(it.at, now) })
}

/** Une barre du graphique : un créneau de temps et ses points. */
data class Bucket(val label: String, val biscotos: Int, val taverne: Int, val traversees: Int = 0)

/** Les deux façons de regarder l'équipage. */
enum class StatsMode(val label: String) {
    /** Tous ensemble : les compteurs cumulés de l'équipage. */
    ENSEMBLE("ENSEMBLE"),

    /** Chacun pour soi : le podium des plus gros biscotos. */
    NO_PAIN("NO PAIN NO GAIN"),
}

object CrewStats {

    /** Le total cumulé de tout l'équipage sur la période. */
    fun teamTotal(crew: List<CrewMate>, period: Period, now: LocalDateTime): Score =
        crew.map { it.score(period, now) }.fold(Score(0, 0), Score::plus)

    /** NO PAIN NO GAIN : les plus gros biscotos d'abord, et à égalité, le moins de taverne gagne. */
    fun noPainRanking(crew: List<CrewMate>, period: Period, now: LocalDateTime): List<Pair<String, Score>> =
        crew.map { it.name to it.score(period, now) }
            .sortedWith(compareByDescending<Pair<String, Score>> { it.second.biscotos }.thenBy { it.second.taverne })

    /** Découpe la période en créneaux : par tranches de 2 h sur la journée, par jour sinon. */
    fun buckets(logs: List<LogEntry>, period: Period, now: LocalDateTime): List<Bucket> {
        val inPeriod = logs.filter { period.contains(it.at, now) }
        fun bucket(label: String, slot: List<LogEntry>) = Score.of(slot).let { Bucket(label, it.biscotos, it.taverne, it.traversees) }
        return if (period == Period.JOUR) {
            (0 until 24 step 2).map { hour ->
                bucket("%02dh".format(hour), inPeriod.filter { it.at.hour in hour until hour + 2 })
            }
        } else {
            (period.days - 1 downTo 0).map { back ->
                val day = now.toLocalDate().minusDays(back)
                bucket("%02d/%02d".format(day.dayOfMonth, day.monthValue), inPeriod.filter { it.at.toLocalDate() == day })
            }
        }
    }
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
    private enum class Temper(val sportChance: Double, val drinkChance: Double, val focusChance: Double) {
        ATHLETE(0.75, 0.15, 0.5),
        POCHARD(0.15, 0.75, 0.25),
        EQUILIBRE(0.45, 0.45, 0.6),
        FLEMMARD(0.10, 0.20, 0.15),
    }

    private val focusMinutes = listOf(5, 10, 30, 30, 45)

    private val sports = Entry.entries.filter { it.side == Side.SPORT }
    private val drinks = Entry.entries.filter { it.side == Side.BOISSON }

    /** Ce que les faux matelots écrivent parfois après une séance... */
    private val sportNotes = listOf(
        "Grosse séance de dos, je sens plus mes bras",
        "Nouveau record de tractions !",
        "Le bloc jaune m'a eu, revanche demain",
        "Abdos en feu 🔥",
        "Séance express avant le boulot",
        "J'ai vu le capitaine pleurer de fierté",
        "Pompes sur le pont, sous la pluie",
    )

    /** ...ou après un passage à la taverne. */
    private val drinkNotes = listOf(
        "Juste une. Promis.",
        "Anniversaire de Barnabé, ça compte pas",
        "Happy hour, c'était obligé",
        "Le cocktail avait une ombrelle, je regrette rien",
        "Apéro sur le port 🌅",
        "On a chanté des chants de marins",
        "Coco m'a regardé bizarrement",
    )

    /** Combien de faux matelots sont tes amis. */
    private const val FRIENDS = 3

    fun generate(now: LocalDateTime, size: Int = 8, random: Random = Random.Default): List<CrewMate> {
        val names = firstNames.shuffled(random).zip(nicknames.shuffled(random)) { first, nick -> "$first $nick" }
        return names.take(size).mapIndexed { index, name ->
            val temper = Temper.entries.random(random)
            val logs = (0L until Period.MOIS.days).flatMap { back ->
                val day = now.toLocalDate().minusDays(back)
                fun at(fromHour: Int, toHour: Int) =
                    day.atTime(random.nextInt(fromHour, toHour), random.nextInt(60))
                val sessions = if (random.nextDouble() < temper.sportChance) random.nextInt(1, 3) else 0
                val rounds = if (random.nextDouble() < temper.drinkChance) random.nextInt(1, 5) else 0
                // Le sport en journée, la taverne le soir.
                fun note(pool: List<String>) = if (random.nextDouble() < 0.3) pool.random(random) else null
                val voyages = if (random.nextDouble() < temper.focusChance) random.nextInt(1, 4) else 0
                List(sessions) { LogEntry(name, sports.random(random), at(7, 20), note(sportNotes)) } +
                    List(rounds) { LogEntry(name, drinks.random(random), at(17, 24), note(drinkNotes)) } +
                    List(voyages) { LogEntry(name, Entry.TRAVERSEE, at(8, 19), FocusLog.note(focusMinutes.random(random))) }
            }.filter { !it.at.isAfter(now) }
            CrewMate(name, logs.sortedByDescending { it.at }, friend = index < FRIENDS)
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

/** Petite icône pour le journal de bord. */
fun Entry.icon(): String = when (this) {
    Entry.MEGA_SEANCE -> "💪"
    Entry.GRIMPE -> "🧗"
    Entry.ABDOS -> "🔥"
    Entry.BIERE -> "🍺"
    Entry.COCKTAIL -> "🍹"
    Entry.VIN -> "🍷"
    Entry.TRAVERSEE -> "⛵"
}

object FocusLog {
    /** Le mot laissé dans le journal de bord après une traversée. */
    fun note(minutes: Int) = "$minutes min de focus"
}
