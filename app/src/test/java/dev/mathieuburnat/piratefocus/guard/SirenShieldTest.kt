package dev.mathieuburnat.piratefocus.guard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SirenShieldTest {

    @Test
    fun `au 5e défilement, Instagram est fermé puis bloqué`() {
        val shield = SirenShield(blockMs = 10_000)
        val verdicts = (1..5).map { shield.onScroll(now = it * 1_000L) }
        assertTrue(verdicts.dropLast(1).none { it })
        assertTrue(verdicts.last())
        assertTrue(shield.isBlocked(now = 14_000))
        assertTrue(shield.onScroll(now = 14_500))
        assertFalse(shield.isBlocked(now = 15_001))
    }

    @Test
    fun `une rafale d'événements compte pour un seul défilement`() {
        val shield = SirenShield()
        repeat(50) { assertFalse(shield.onScroll(now = 1_000L + it * 20)) }
        assertFalse(shield.onScroll(now = 5_000))
    }

    @Test
    fun `quitter Instagram remet les défilements à zéro`() {
        val shield = SirenShield()
        (1..4).forEach { shield.onScroll(now = it * 1_000L) }
        shield.onLeave()
        assertFalse(shield.onScroll(now = 10_000))
    }

    @Test
    fun `reconnaît les stories, pas les Reels`() {
        assertTrue(SirenShield.isStory(sequenceOf("com.instagram.android:id/action_bar", "com.instagram.android:id/reel_viewer_root")))
        assertFalse(SirenShield.isStory(sequenceOf("com.instagram.android:id/clips_viewer_view_pager", "com.instagram.android:id/feed")))
    }
}
