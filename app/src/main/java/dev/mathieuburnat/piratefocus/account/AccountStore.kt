package dev.mathieuburnat.piratefocus.account

import android.content.Context

/** Le compte rangé sur le téléphone (pseudo, jeton de session). */
class AccountStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Account = Account(
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false),
        pirateName = prefs.getString(KEY_NAME, null).orEmpty(),
        token = prefs.getString(KEY_TOKEN, null),
        email = prefs.getString(KEY_EMAIL, null),
        login = prefs.getString(KEY_LOGIN, null),
        kind = prefs.getString(KEY_KIND, null),
        google = prefs.getBoolean(KEY_GOOGLE, false),
        public = prefs.getBoolean(KEY_PUBLIC, true),
    )

    fun save(account: Account) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDED, account.onboarded)
            .putString(KEY_NAME, account.pirateName)
            .putString(KEY_TOKEN, account.token)
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_LOGIN, account.login)
            .putString(KEY_KIND, account.kind)
            .putBoolean(KEY_GOOGLE, account.google)
            .putBoolean(KEY_PUBLIC, account.public)
            .apply()
    }

    companion object {
        const val PREFS = "compte"
        private const val KEY_ONBOARDED = "accueil_passe"
        private const val KEY_NAME = "pseudo"
        private const val KEY_TOKEN = "jeton"
        private const val KEY_EMAIL = "email"
        private const val KEY_LOGIN = "identifiant"
        private const val KEY_KIND = "type"
        private const val KEY_GOOGLE = "google_lie"
        private const val KEY_PUBLIC = "public"
    }
}
