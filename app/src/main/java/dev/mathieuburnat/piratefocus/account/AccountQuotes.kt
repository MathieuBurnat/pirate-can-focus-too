package dev.mathieuburnat.piratefocus.account

/** Ce que le capitaine raconte à propos des comptes. */
object AccountQuotes {

    const val WELCOME = "Ahoy ! Sur ce navire, personne n'est forcé de signer quoi que ce soit.\n\n" +
        "Tu peux utiliser l'appli sans créer de compte. Bonne navigation, matelot !"

    const val ANONYMOUS = "Un pseudo, et tu montes à bord. Aucune adresse, aucun nom, rien."

    const val FREE_ACCOUNT =
        "Compte 100 % gratuit : un email et un mot de passe. L'email sert juste d'identifiant, " +
            "jamais vérifié, et personne ne t'écrira. Ton butin est mis à l'abri en ligne et te suit sur un autre téléphone."

    /** Le code secret, montré une seule fois. */
    const val RECOVERY_WARNING =
        "Note ce code secret en lieu sûr : c'est la seule clé de ton coffre si tu oublies ton mot de passe. " +
            "Le capitaine ne le montrera plus jamais ! Code perdu et mot de passe oublié = compte perdu."

    /** Le petit mot sous un nouveau mot de passe, selon sa solidité. */
    fun strength(strength: PasswordStrength): String = when (strength) {
        PasswordStrength.WEAK -> "Mot de passe faible, matelot ! Même un mousse le devinerait."
        PasswordStrength.MEDIUM -> "Pas mal, mais ajoute des majuscules, des chiffres ou des symboles."
        PasswordStrength.STRONG -> "Solide comme une coque de chêne !"
        PasswordStrength.VERY_STRONG -> "Coffre-fort digne de Barbe-Noire !"
    }

    const val PASSWORDS_DIFFER = "Les deux mots de passe ne correspondent pas."

    const val FORGOT_EXPLAINED = "Ton email, ton code secret, et un nouveau mot de passe. Un nouveau code secret te sera donné."

    const val LOGIN_EXPLAINED = "Ton email reste privé : les autres pirates ne voient que ton pseudo."

    /** Le petit mot pour les matelots sans compte : informatif, pas alarmiste. */
    const val NO_ACCOUNT_INFO =
        "Bon à savoir, matelot : sans compte, ton butin est rangé dans la cale de ce téléphone. " +
            "Si le navire coule, le butin coule avec, sauf si tu fais une sauvegarde. " +
            "Tu pourras créer un compte gratuit quand tu veux."

    const val PUBLIC_EXPLAINED = "Tes exploits apparaissent dans les statistiques de l'équipage, sous ton pseudo."

    const val PRIVATE_EXPLAINED = "Tes exploits sont sauvegardés, mais tu restes caché dans la cale : personne ne les voit."

    const val DELETE_WARNING =
        "Ton email et ta connexion seront jetés par-dessus bord, et tes petits mots effacés. " +
            "Tes exploits resteront au classement sous le nom d'un « Ancien matelot ». Le journal reste sur ce téléphone."

    const val FLYING = "~ le perroquet porte le message... ~"
}
