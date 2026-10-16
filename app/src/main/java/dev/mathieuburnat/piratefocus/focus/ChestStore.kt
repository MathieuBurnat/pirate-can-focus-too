package dev.mathieuburnat.piratefocus.focus

import android.content.Context

/** Le coffre du capitaine : doublons et traversées, gardés sur le téléphone. */
class ChestStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Le minuteur au port, avec le butin déjà amassé. */
    fun load(): FocusState = FocusState(
        doubloons = prefs.getInt(KEY_DOUBLOONS, 0),
        voyages = prefs.getInt(KEY_VOYAGES, 0),
    )

    fun save(doubloons: Int, voyages: Int) {
        prefs.edit().putInt(KEY_DOUBLOONS, doubloons).putInt(KEY_VOYAGES, voyages).apply()
    }

    companion object {
        const val PREFS = "coffre"
        private const val KEY_DOUBLOONS = "doublons"
        private const val KEY_VOYAGES = "traversees"
    }
}
