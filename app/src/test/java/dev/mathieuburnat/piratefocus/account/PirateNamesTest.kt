package dev.mathieuburnat.piratefocus.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PirateNamesTest {

    @Test
    fun `un pseudo tiré au sort a un titre, un prénom et un surnom`() {
        val random = Random(42)
        repeat(50) {
            val name = PirateNames.random(random)
            assertTrue(name, name.split(" ").size >= 3)
            assertTrue(name, name.length <= PirateNames.MAX_LENGTH)
        }
    }

    @Test
    fun `un pseudo choisi est nettoyé ou refusé`() {
        assertEquals("Anne la Terrible", PirateNames.clean("  Anne   la Terrible "))
        assertNull(PirateNames.clean("x"))
        assertNull(PirateNames.clean("a".repeat(49)))
    }
}
