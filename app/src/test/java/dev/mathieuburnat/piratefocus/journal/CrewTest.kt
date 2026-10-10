package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CrewTest {

    @Test
    fun `un équipage de huit matelots aux noms uniques`() {
        val crew = FakeCrew.generate(random = Random(42))
        assertEquals(8, crew.size)
        assertEquals(8, crew.map { it.name }.toSet().size)
    }

    @Test
    fun `le mois contient la semaine qui contient le jour`() {
        FakeCrew.generate(random = Random(7)).forEach { mate ->
            val day = mate.score(Period.JOUR)
            val week = mate.score(Period.SEMAINE)
            val month = mate.score(Period.MOIS)
            assertTrue(day.biscotos <= week.biscotos && week.biscotos <= month.biscotos)
            assertTrue(day.taverne <= week.taverne && week.taverne <= month.taverne)
        }
    }

    @Test
    fun `le verdict d'un score suit les règles du journal`() {
        assertEquals(Verdict.EPONGE, Score(0, 4).verdict)
        assertEquals(Verdict.SPORTIF, Score(5, 2).verdict)
    }
}
