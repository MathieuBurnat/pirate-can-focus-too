package dev.mathieuburnat.piratefocus.settings

import android.content.Context
import dev.mathieuburnat.piratefocus.account.AccountStore
import dev.mathieuburnat.piratefocus.focus.ChestStore
import dev.mathieuburnat.piratefocus.guard.BlacklistStore
import dev.mathieuburnat.piratefocus.guard.FocusGuardService
import dev.mathieuburnat.piratefocus.guard.GuardNudgeStore
import dev.mathieuburnat.piratefocus.journal.JournalStore

/**
 * Factory reset : tout ce que le téléphone garde part par-dessus bord (compte local, journal, coffre,
 * mode dev, liste noire). Les données d'un compte gratuit restent chez le Worker.
 */
object FactoryReset {

    /** Les SharedPreferences rangées par l'app. */
    val PREFS = listOf(AccountStore.PREFS, ChestStore.PREFS, DevModeStore.PREFS, BlacklistStore.PREFS, GuardNudgeStore.PREFS)

    fun wipe(context: Context) {
        FocusGuardService.stop(context)
        PREFS.forEach { context.applicationContext.deleteSharedPreferences(it) }
        JournalStore.journalFile(context).delete()
    }
}
