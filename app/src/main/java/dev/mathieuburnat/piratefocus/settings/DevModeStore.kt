package dev.mathieuburnat.piratefocus.settings

import android.content.Context

/**
 * Le mode dev et son option « fake data » : cochée, les statistiques de l'équipage montrent des matelots inventés
 * (codés en dur dans l'app). Sinon, elles montrent les vrais pirates publics du Worker.
 * La base, elle, ne contient jamais de fausses données. Éteindre le mode dev décoche aussi « fake data ».
 */
class DevModeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOn(): Boolean = prefs.getBoolean(KEY, false)

    fun set(on: Boolean) {
        val edit = prefs.edit().putBoolean(KEY, on)
        if (!on) edit.putBoolean(FAKE_DATA, false)
        edit.apply()
    }

    /** Les fausses données ne servent que mode dev allumé. */
    fun isFakeData(): Boolean = isOn() && prefs.getBoolean(FAKE_DATA, false)

    /** Le bouclier anti-sirènes (stories d'Instagram refermées, Instagram chassé au 5e défilement) : mode dev seulement. */
    fun isSirenShield(): Boolean = isOn() && prefs.getBoolean(SIREN_SHIELD, false)

    fun setSirenShield(on: Boolean) {
        prefs.edit().putBoolean(SIREN_SHIELD, on).apply()
    }

    fun setFakeData(on: Boolean) {
        prefs.edit().putBoolean(FAKE_DATA, on).apply()
    }

    /**
     * Le bouton « Mode dev » est caché tant qu'on n'a pas tapé 5 fois sur le titre des paramètres,
     * et il se recache dès qu'on éteint le mode dev.
     */
    fun isRevealed(): Boolean = prefs.getBoolean(REVEALED, false) || isOn()

    fun reveal() {
        prefs.edit().putBoolean(REVEALED, true).apply()
    }

    fun hide() {
        prefs.edit().putBoolean(REVEALED, false).apply()
    }

    companion object {
        const val PREFS = "parametres"
        private const val KEY = "mode_dev"
        private const val REVEALED = "mode_dev_visible"
        private const val FAKE_DATA = "mode_dev_fake_data"
        private const val SIREN_SHIELD = "mode_dev_bouclier_sirenes"
    }
}
