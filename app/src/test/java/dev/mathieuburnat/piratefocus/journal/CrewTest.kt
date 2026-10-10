package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.random.Random

class CrewTest {

    private val now = LocalDateTime.of(2026, 10, 10, 11, 0)

    @Test
    fun `un équipage de huit matelots aux noms uniques`() {
        val crew = FakeCrew.generate(now, random = Random(42))
        assertEquals(8, crew.size)
        assertEquals(8, crew.map { it.name }.toSet().size)
    }

    @Test
    fun `aucune activité dans le futur`() {
        FakeCrew.generate(now, random = Random(3)).flatMap { it.logs }.forEach { assertFalse(it.at.isAfter(now)) }
    }

    @Test
    fun `le mois contient la semaine qui contient le jour`() {
        FakeCrew.generate(now, random = Random(7)).forEach { mate ->
            val day = mate.score(Period.JOUR, now)
            val week = mate.score(Period.SEMAINE, now)
            val month = mate.score(Period.MOIS, now)
            assertTrue(day.biscotos <= week.biscotos && week.biscotos <= month.biscotos)
            assertTrue(day.taverne <= week.taverne && week.taverne <= month.taverne)
        }
    }

    @Test
    fun `les périodes`() {
        assertTrue(Period.JOUR.contains(now.withHour(0), now))
        assertFalse(Period.JOUR.contains(now.minusDays(1), now))
        assertTrue(Period.SEMAINE.contains(now.minusDays(6), now))
        assertFalse(Period.SEMAINE.contains(now.minusDays(7), now))
    }

    @Test
    fun `le graphique compte chaque activité une seule fois`() {
        val logs = listOf(
            LogEntry("Toi", Entry.BIERE, now.withHour(9)),
            LogEntry("Toi", Entry.GRIMPE, now.withHour(8)),
            LogEntry("Toi", Entry.VIN, now.minusDays(2)),
        )
        val day = CrewStats.buckets(logs, Period.JOUR, now)
        assertEquals(12, day.size)
        assertEquals(Bucket("08h", 1, 1), day[4])
        val week = CrewStats.buckets(logs, Period.SEMAINE, now)
        assertEquals(7, week.size)
        assertEquals(2, week.sumOf { it.taverne })
        assertEquals("10/10", week.last().label)
    }

    @Test
    fun `le verdict d'un score suit les règles du journal`() {
        assertEquals(Verdict.EPONGE, Score(0, 4).verdict)
        assertEquals(Verdict.SPORTIF, Score(5, 2).verdict)
    }
}
