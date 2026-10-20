package dev.mathieuburnat.piratefocus.guard

import android.content.Context
import kotlin.random.Random

/**
 * Le capitaine réclame ses autorisations en arrivant sur l'écran de focus : toujours la première fois,
 * puis environ une fois sur [EVERY] (logique pure, testée). Jamais si le gardien est prêt
 * ou si une traversée est en cours.
 */
object GuardNudge {

    const val EVERY = 3

    fun shouldShow(guardReady: Boolean, voyageRunning: Boolean, firstTime: Boolean, random: Random = Random.Default): Boolean =
        !guardReady && !voyageRunning && (firstTime || random.nextInt(EVERY) == 0)
}

/** Le capitaine a-t-il déjà fait sa première demande d'autorisations ? (effacé par le factory reset) */
class GuardNudgeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isFirstTime(): Boolean = !prefs.getBoolean(KEY_ASKED, false)

    fun markAsked() {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
    }

    companion object {
        const val PREFS = "gardien"
        private const val KEY_ASKED = "deja_demande"
    }
}
