package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JournalTest {

    private fun journal(vararg entries: Entry) = entries.fold(JournalState()) { state, entry -> state.add(entry) }

    @Test
    fun `les verdicts du capitaine`() {
        assertEquals(Verdict.PAGE_BLANCHE, journal().verdict)
        assertEquals(Verdict.ATHLETE, journal(Entry.GRIMPE).verdict)
        assertEquals(Verdict.PILIER_DE_TAVERNE, journal(Entry.BIERE).verdict)
        assertEquals(Verdict.EPONGE, journal(Entry.BIERE, Entry.VIN, Entry.COCKTAIL).verdict)
        assertEquals(Verdict.EQUILIBRE, journal(Entry.ABDOS, Entry.VIN).verdict)
        assertEquals(Verdict.SPORTIF, journal(Entry.ABDOS, Entry.MEGA_SEANCE, Entry.VIN).verdict)
        assertEquals(Verdict.PILIER_DE_TAVERNE, journal(Entry.ABDOS, Entry.VIN, Entry.BIERE).verdict)
    }

    @Test
    fun `les totaux par camp`() {
        val state = journal(Entry.BIERE, Entry.BIERE, Entry.GRIMPE)
        assertEquals(2, state.total(Side.BOISSON))
        assertEquals(1, state.total(Side.SPORT))
        assertEquals(2, state.count(Entry.BIERE))
    }

    @Test
    fun `rayer une ligne ne descend pas sous zéro`() {
        val state = journal(Entry.VIN).remove(Entry.VIN).remove(Entry.VIN)
        assertEquals(0, state.count(Entry.VIN))
    }

    @Test
    fun `passé trois verres le capitaine refuse sauf si on insiste`() {
        val twoDrinks = journal(Entry.BIERE, Entry.VIN)
        val threeDrinks = twoDrinks.add(Entry.COCKTAIL)
        assertEquals(false, JournalRules.refuses(Entry.BIERE, twoDrinks, insisting = false))
        assertEquals(true, JournalRules.refuses(Entry.BIERE, threeDrinks, insisting = false))
        assertEquals(false, JournalRules.refuses(Entry.BIERE, threeDrinks, insisting = true))
        assertEquals(false, JournalRules.refuses(Entry.GRIMPE, threeDrinks, insisting = false))
    }

    @Test
    fun `le capitaine intervient tous les cinq verres`() {
        assertNull(JournalQuotes.intervention(4))
        assertNotNull(JournalQuotes.intervention(5))
        assertNotNull(JournalQuotes.intervention(10))
    }

    @Test
    fun `on frappe sept fois à la porte du journal`() {
        assertNull(JournalQuotes.knock(6))
        assertNotNull(JournalQuotes.knock(1))
    }
}
