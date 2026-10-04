package dev.mathieuburnat.piratefocus.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTimerTest {

    @Test
    fun `finir une traversée rapporte des doublons et passe en escale`() {
        var state = FocusTimer.setSail(FocusTimer.selectDuration(FocusState(), 15))
        repeat(15 * 60) { state = FocusTimer.tick(state) }

        assertEquals(Phase.BREAK, state.phase)
        assertEquals(15, state.doubloons)
        assertEquals(1, state.voyages)
        assertEquals(5 * 60, state.remainingSeconds)
    }

    @Test
    fun `la fin de l'escale ramène au port`() {
        var state = FocusState(phase = Phase.BREAK, remainingSeconds = 1, totalSeconds = 300)
        state = FocusTimer.tick(state)

        assertEquals(Phase.IDLE, state.phase)
        assertEquals(30 * 60, state.remainingSeconds)
    }

    @Test
    fun `une durée sur mesure est bornée`() {
        assertEquals(45, FocusTimer.selectDuration(FocusState(), 45).focusMinutes)
        assertEquals(FocusTimer.MAX_MINUTES, FocusTimer.selectDuration(FocusState(), 999).focusMinutes)
        assertEquals(FocusTimer.MIN_MINUTES, FocusTimer.selectDuration(FocusState(), 0).focusMinutes)
    }

    @Test
    fun `abandonner coule le navire sans récompense`() {
        val state = FocusTimer.abandonShip(FocusTimer.setSail(FocusState()))

        assertEquals(Phase.SUNK, state.phase)
        assertEquals(0, state.doubloons)
    }

    @Test
    fun `les longues traversées ont un bonus`() {
        assertEquals(60, FocusTimer.rewardFor(50))
        assertEquals(30, FocusTimer.rewardFor(30))
    }

    @Test
    fun `format en minutes et secondes`() {
        assertEquals("12:34", FocusTimer.format(754))
    }
}
