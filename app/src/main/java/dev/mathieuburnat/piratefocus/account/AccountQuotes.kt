package dev.mathieuburnat.piratefocus.account

/** Ce que le capitaine raconte à propos des comptes. */
object AccountQuotes {

    const val WELCOME = "Ahoy ! Sur ce navire, personne n'est forcé de signer quoi que ce soit.\n\n" +
        "Tu peux utiliser l'appli sans créer de compte. Bonne navigation, matelot !"

    const val ANONYMOUS = "Un pseudo, et tu montes à bord. Aucune adresse, aucun nom, rien."

    const val FREE_ACCOUNT =
        "Compte 100 % gratuit, sans email : un identifiant et un mot de passe. " +
            "Ton butin est mis à l'abri en ligne et te suit sur un autre téléphone."

    /** Le code secret, montré une seule fois. */
    const val RECOVERY_WARNING =
        "Note ce code secret en lieu sûr : c'est la seule clé de ton coffre si tu oublies ton mot de passe. " +
            "Le capitaine ne le montrera plus jamais ! Code perdu et mot de passe oublié = compte perdu."

    const val FORGOT_EXPLAINED = "Ton identifiant, ton code secret, et un nouveau mot de passe. Un nouveau code secret te sera donné."

    const val LOGIN_EXPLAINED = "L'identifiant reste privé : les autres pirates ne voient que ton pseudo."

    /** Le petit mot pour les matelots sans compte : informatif, pas alarmiste. */
    const val NO_ACCOUNT_INFO =
        "Bon à savoir, matelot : sans compte, ton butin est rangé dans la cale de ce téléphone. " +
            "Si le navire coule, le butin coule avec, sauf si tu fais une sauvegarde. " +
            "Tu pourras créer un compte gratuit quand tu veux."

    const val PUBLIC_EXPLAINED = "Tes exploits apparaissent dans les statistiques de l'équipage, sous ton pseudo."

    const val PRIVATE_EXPLAINED = "Tes exploits sont sauvegardés, mais tu restes caché dans la cale : personne ne les voit."

    const val DELETE_WARNING =
        "Ton identifiant et ta connexion seront jetés par-dessus bord, et tes petits mots effacés. " +
            "Tes exploits resteront au classement sous le nom d'un « Ancien matelot ». Le journal reste sur ce téléphone."

    const val FLYING = "~ le perroquet porte le message... ~"
}
