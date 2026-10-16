package dev.mathieuburnat.piratefocus.guard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GuardNudgeTest {

    @Test
    fun `la première fois, le capitaine demande toujours`() {
        repeat(20) { seed ->
            assertTrue(GuardNudge.shouldShow(guardReady = false, voyageRunning = false, firstTime = true, random = Random(seed)))
        }
    }

    @Test
    fun `gardien prêt ou traversée en cours, le capitaine se tait`() {
        assertFalse(GuardNudge.shouldShow(guardReady = true, voyageRunning = false, firstTime = true))
        assertFalse(GuardNudge.shouldShow(guardReady = false, voyageRunning = true, firstTime = true))
    }

    @Test
    fun `ensuite, environ une fois sur trois`() {
        val random = Random(7)
        val shown = (1..3_000).count { GuardNudge.shouldShow(guardReady = false, voyageRunning = false, firstTime = false, random = random) }
        assertTrue("$shown", shown in 850..1_150)
    }
}
