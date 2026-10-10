package dev.mathieuburnat.piratefocus.account

import android.content.Context

/** Le compte rangé sur le téléphone (pseudo, jeton de session). */
class AccountStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("compte", Context.MODE_PRIVATE)

    fun load(): Account = Account(
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false),
        pirateName = prefs.getString(KEY_NAME, null).orEmpty(),
        token = prefs.getString(KEY_TOKEN, null),
        email = prefs.getString(KEY_EMAIL, null),
        kind = prefs.getString(KEY_KIND, null),
        public = prefs.getBoolean(KEY_PUBLIC, true),
    )

    fun save(account: Account) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDED, account.onboarded)
            .putString(KEY_NAME, account.pirateName)
            .putString(KEY_TOKEN, account.token)
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_KIND, account.kind)
            .putBoolean(KEY_PUBLIC, account.public)
            .apply()
    }

    private companion object {
        const val KEY_ONBOARDED = "accueil_passe"
        const val KEY_NAME = "pseudo"
        const val KEY_TOKEN = "jeton"
        const val KEY_EMAIL = "email"
        const val KEY_KIND = "type"
        const val KEY_PUBLIC = "public"
    }
}
