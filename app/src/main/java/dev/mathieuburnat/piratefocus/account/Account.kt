package dev.mathieuburnat.piratefocus.account

/** Le compte du matelot, tel que le téléphone le connaît. */
data class Account(
    /** L'écran de bienvenue a été passé (anonyme ou compte gratuit). */
    val onboarded: Boolean = false,
    val pirateName: String = "",
    /** Jeton de session du Worker : null tant qu'on n'a pas de compte gratuit. */
    val token: String? = null,
    val email: String? = null,
    /** "email" ou "google". */
    val kind: String? = null,
    /** Visible dans les statistiques de l'équipage. */
    val public: Boolean = true,
) {
    /** Sans compte, rien ne quitte le téléphone. */
    val signedIn: Boolean get() = token != null
}
