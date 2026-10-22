package dev.mathieuburnat.piratefocus.guard

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import dev.mathieuburnat.piratefocus.focus.PirateQuotes
import dev.mathieuburnat.piratefocus.settings.DevModeStore

/**
 * Le service d'accessibilité du bouclier anti-sirènes (mode dev, voir [SirenShield]) : il regarde Instagram,
 * referme les stories et chasse le matelot qui défile trop. Il ne fait rien tant que l'option est éteinte.
 */
class SirenShieldService : AccessibilityService() {

    private val shield = SirenShield()
    private val devMode by lazy { DevModeStore(this) }
    private var lastStoryCheck = 0L
    private var lastAction = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val app = event.packageName?.toString() ?: return
        if (!devMode.isSirenShield()) return
        val now = SystemClock.elapsedRealtime()

        if (app != SirenShield.INSTAGRAM) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && app !in PASSING_BY) shield.onLeave()
            return
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (shield.onScroll(now)) sendAway(now)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (shield.isBlocked(now)) return sendAway(now)
                if (now - lastStoryCheck < STORY_CHECK_EVERY_MS) return
                lastStoryCheck = now
                if (storyOpen()) closeStory(now)
            }
        }
    }

    /** Une story est-elle à l'écran ? On fouille les identifiants des vues d'Instagram (sans lire leur contenu). */
    private fun storyOpen(): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != SirenShield.INSTAGRAM) return false
        return SirenShield.isStory(viewIds(root))
    }

    private fun viewIds(root: AccessibilityNodeInfo): Sequence<String> = sequence {
        val queue = ArrayDeque(listOf(root))
        var seen = 0
        while (queue.isNotEmpty() && seen < MAX_NODES) {
            val node = queue.removeFirst()
            seen++
            node.viewIdResourceName?.let { yield(it) }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
    }

    private fun closeStory(now: Long) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        if (now - lastAction > TOAST_EVERY_MS) Toast.makeText(this, PirateQuotes.sirenStory(), Toast.LENGTH_SHORT).show()
        lastAction = now
    }

    /** Retour à l'écran d'accueil, et le capitaine surgit. */
    private fun sendAway(now: Long) {
        if (now - lastAction < KICK_EVERY_MS) return
        lastAction = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(CaughtActivity.sirenIntent(this))
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val STORY_CHECK_EVERY_MS = 300L
        private const val TOAST_EVERY_MS = 3_000L
        private const val KICK_EVERY_MS = 1_500L
        private const val MAX_NODES = 600

        /** Ce qui passe par-dessus Instagram sans qu'on l'ait quitté (barre système, clavier...). */
        private val PASSING_BY = setOf(SirenShield.INSTAGRAM, "com.android.systemui", "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin")

        /** Le service est-il activé dans les réglages d'accessibilité du téléphone ? */
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, SirenShieldService::class.java)
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
