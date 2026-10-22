package dev.mathieuburnat.piratefocus.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PirateLevelTest {

    @Test
    fun `le niveau monte avec les doublons, de plus en plus lentement`() {
        assertEquals(1, PirateLevel.of(0))
        assertEquals(1, PirateLevel.of(49))
        assertEquals(2, PirateLevel.of(50))
        assertEquals(4, PirateLevel.of(499))
        assertEquals(5, PirateLevel.of(500))
        assertEquals(10, PirateLevel.of(2_250))
        assertEquals(0.5f, PirateLevel.progress(100), 0.001f)
    }

    @Test
    fun `avant le niveau 5, seules les têtes de départ (garçon, fille, je ne dis rien)`() {
        listOf(0, 50, 100).forEach { assertEquals(emptyList<Int>(), PirateLevel.choose(it, emptyList(), level = 4)) }
        assertNull(PirateLevel.choose(7, emptyList(), level = 4))
    }

    @Test
    fun `au niveau 5, trois têtes au choix, puis on passe librement de l'une à l'autre`() {
        var unlocked = emptyList<Int>()
        for (head in listOf(3, 9, 12)) unlocked = PirateLevel.choose(head, unlocked, level = 5)!!
        assertEquals(listOf(3, 9, 12), unlocked)
        assertNull(PirateLevel.choose(20, unlocked, level = 9))
        assertEquals(unlocked, PirateLevel.choose(9, unlocked, level = 5))
        assertEquals(0, PirateLevel.freeSlots(unlocked, level = 5))
    }

    @Test
    fun `au niveau 10, toutes les têtes sans compter`() {
        assertTrue(PirateLevel.canWear(42, listOf(3, 9, 12), level = 10))
        assertEquals(listOf(3, 9, 12), PirateLevel.choose(42, listOf(3, 9, 12), level = 10))
    }
}

class PirateHeadsTest {

    @Test
    fun `cinquante têtes de 16 sur 16 par sorte de pirate, toutes différentes`() {
        assertEquals(150, PirateHeads.all.size)
        PirateHeads.Kind.entries.forEach { assertEquals(PirateHeads.PER_KIND, PirateHeads.ofKind(it).size) }
        assertEquals(PirateHeads.Kind.FILLE, PirateHeads[50].kind)
        assertEquals(PirateHeads.Kind.ANDROGYNE, PirateHeads[149].kind)
        PirateHeads.all.forEachIndexed { id, head ->
            assertEquals(id, head.id)
            assertEquals(16, head.sprite.size)
            head.sprite.forEach { assertEquals("tête $id : $it", 16, it.length) }
            head.sprite.joinToString("").filter { it != '.' }.forEach { assertTrue("tête $id : $it", it in head.colors) }
        }
        assertEquals(PirateHeads.COUNT, PirateHeads.all.map { it.look }.toSet().size)
    }

    @Test
    fun `les mousses au bandana au départ, et personne ne porte la tête du capitaine`() {
        listOf(0, 50, 100).forEach {
            assertEquals(PirateHeads.Hat.BANDANA, PirateHeads[it].hat)
            assertEquals(PirateHeads.Eyes.TWO_EYES, PirateHeads[it].eyes)
        }
        assertEquals(PirateHeads[0], PirateHeads[999])
        val captain = PirateHeads.Head(-1, PirateHeads.Hat.TRICORNE_SKULL, PirateHeads.Eyes.PATCH_LEFT, PirateHeads.Beard.FULL, 0, 0, 0, 0, earring = true)
        assertTrue(PirateHeads.all.none { it.look == captain.look })
    }

    @Test
    fun `les têtes gardent leur numéro d'une version à l'autre`() {
        // Ces numéros sont rangés chez le Worker : si ce test casse, l'ordre des têtes a changé.
        val signature = listOf(1, 2, 25, 49).joinToString(" | ") { id ->
            with(PirateHeads[id]) { "$hat $eyes $beard $skin$beardColor$hatColor$band${if (earring) "G" else "-"}" }
        }
        assertEquals(
            "TRICORNE PATCH_LEFT MOUSTACHE 1110G | BANDANA TWO_EYES SHAVED 1002G | " +
                "BALD PATCH_LEFT MOUSTACHE 0002G | TRICORNE_SKULL TWO_EYES GOATEE 0301G",
            signature,
        )
    }
}

class CaptainHelloTest {

    @Test
    fun `environ une fois sur cinq, et jamais sans pseudo`() {
        val random = Random(5)
        val greeted = (1..5_000).count { CaptainHello.shouldGreet("Anne Bonny", random) }
        assertTrue("$greeted", greeted in 850..1_150)
        assertFalse((1..100).any { CaptainHello.shouldGreet(" ", Random(it)) })
    }

    @Test
    fun `le capitaine dit le pseudo`() {
        repeat(10) { assertTrue(CaptainHello.greeting("Calico Jack", Random(it)).contains("Calico Jack")) }
    }
}
