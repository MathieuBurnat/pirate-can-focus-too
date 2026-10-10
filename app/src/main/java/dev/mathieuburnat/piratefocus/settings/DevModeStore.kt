package dev.mathieuburnat.piratefocus.settings

import android.content.Context

/**
 * Le mode dev : les statistiques de l'équipage montrent des matelots inventés (codés en dur dans l'app).
 * Hors mode dev, elles montrent les vrais pirates publics du Worker. La base, elle, ne contient jamais de fausses données.
 */
class DevModeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("parametres", Context.MODE_PRIVATE)

    fun isOn(): Boolean = prefs.getBoolean(KEY, false)

    fun set(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
    }

    private companion object {
        const val KEY = "mode_dev"
    }
}
