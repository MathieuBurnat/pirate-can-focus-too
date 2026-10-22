package dev.mathieuburnat.piratefocus.guard

/**
 * Le bouclier anti-sirènes (mode dev) : les stories d'Instagram ne s'ouvrent pas, et Instagram est fermé au
 * [SCROLL_LIMIT]e défilement, puis bloqué [blockMs] ms (30 minutes par défaut). Logique pure, testée ; le service d'accessibilité
 * [SirenShieldService] lui raconte ce qu'il voit.
 */
class SirenShield(
    private val scrollLimit: Int = SCROLL_LIMIT,
    private val gestureGapMs: Long = GESTURE_GAP_MS,
    /** Durée du blocage, réglable dans la fenêtre du bouclier. */
    var blockMs: Long = BLOCK_MS,
) {
    private var scrolls = 0
    private var lastScrollAt = Long.MIN_VALUE / 2

    /** Fin du blocage en cours (rangée sur le téléphone : la fenêtre du bouclier l'affiche et peut la lever). */
    var blockedUntil = 0L

    fun isBlocked(now: Long): Boolean = now < blockedUntil

    /**
     * Instagram a défilé. Une rafale d'événements rapprochés compte pour un seul coup de pouce.
     * Vrai s'il faut fermer Instagram (limite atteinte, ou déjà bloqué).
     */
    fun onScroll(now: Long): Boolean {
        if (isBlocked(now)) return true
        if (now - lastScrollAt > gestureGapMs) scrolls++
        lastScrollAt = now
        if (scrolls < scrollLimit) return false
        scrolls = 0
        blockedUntil = now + blockMs
        return true
    }

    /** Le matelot est passé à une autre appli : les défilements repartent de zéro (le blocage, lui, reste). */
    fun onLeave() {
        scrolls = 0
    }

    companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val SCROLL_LIMIT = 5
        const val GESTURE_GAP_MS = 700L
        const val BLOCK_MS = 30 * 60 * 1000L

        /** Les durées de blocage proposées, en minutes. */
        val BLOCK_CHOICES = listOf(5, 15, 30, 60)

        /** Les écrans de stories d'Instagram (« reel » dans son code, à ne pas confondre avec les Reels, « clips »). */
        private val STORY_IDS = listOf("reel_viewer", "story_viewer")

        fun isStory(viewIds: Sequence<String>): Boolean = viewIds.any { id -> STORY_IDS.any { it in id } }
    }
}
