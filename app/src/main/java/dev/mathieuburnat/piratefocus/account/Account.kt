package dev.mathieuburnat.piratefocus.account

/** Le compte du matelot, tel que le téléphone le connaît. */
data class Account(
    /** L'écran de bienvenue a été passé (anonyme ou compte gratuit). */
    val onboarded: Boolean = false,
    val pirateName: String = "",
    /** Jeton de session du Worker : null tant qu'on n'a pas de compte gratuit. */
    val token: String? = null,
    val email: String? = null,
    /** L'email qui sert d'identifiant au compte gratuit (privé, jamais vérifié ni montré aux autres pirates). */
    val login: String? = null,
    /** "password" (identifiant + mot de passe), ou les anciens "email" et "google". */
    val kind: String? = null,
    /** Un compte Google est lié : « CONTINUER AVEC GOOGLE » mène à ce pirate ([email] est alors l'adresse Google). */
    val google: Boolean = false,
    /** Visible dans les statistiques de l'équipage. */
    val public: Boolean = true,
    /** La tête portée (numéro dans PirateHeads, 0 = celle du capitaine). */
    val head: Int = 0,
    /** Les têtes débloquées au niveau 5 (3 au choix). */
    val heads: List<Int> = emptyList(),
) {
    /** Sans compte, rien ne quitte le téléphone. */
    val signedIn: Boolean get() = token != null
}
