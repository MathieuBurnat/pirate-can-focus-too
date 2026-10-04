package dev.mathieuburnat.piratefocus.focus

import kotlin.random.Random

object PirateQuotes {

    private val quotes = mapOf(
        Phase.IDLE to listOf(
            "Moussaillon ! On lève l'ancre ou on bronze sur le quai ?",
            "Un vrai pirate ne scrolle pas. Il navigue.",
            "Le trésor ne va pas se déterrer tout seul, sacrebleu !",
            "Choisis ta traversée, et que Neptune t'épargne les notifs.",
            "Le perroquet s'impatiente. Il a déjà mangé trois biscuits.",
            "Ce navire ne va pas se piloter tout seul. Enfin si, mais c'est moins drôle.",
            "J'ai vu des méduses plus motivées que toi. Allez, hop !",
            "La mer est calme, le vent est bon, ton excuse est mauvaise.",
            "Arr ! Chaque minute à quai coûte un doublon à mon moral.",
            "Même le kraken fait ses tâches avant de dormir.",
            "Un pirate qui procrastine, c'est juste un marin en pyjama.",
            "La carte au trésor dit : « commence maintenant ». C'est écrit en gros.",
            "Mon cache-œil cache un œil, pas ta to-do list.",
            "Les mouettes rigolent. Montre-leur de quel bois tu te chauffes.",
            "Lève l'ancre avant que la rouille ne te lève, toi.",
            "Hissez les voiles, matelot ! Enfin... appuie sur le bouton.",
            "On dit que les grands capitaines commencent par cinq minutes.",
            "Le rhum attendra. Le travail, lui, non.",
            "J'ai traversé sept mers. Toi, traverse juste cette tâche.",
            "Ta jambe de bois n'est pas une excuse. Tu n'en as même pas.",
        ),
        Phase.FOCUS to listOf(
            "Silence à bord ! Le capitaine se concentre.",
            "Pas touche au téléphone, ou c'est la planche !",
            "Garde le cap, moussaillon. Les sirènes d'Instagram mentent.",
            "Rame, rame, rame... enfin, travaille quoi.",
            "Mille sabords, quelle concentration ! Continue.",
        ),
        Phase.BREAK to listOf(
            "Escale au port ! Un verre de jus de coco pour le héros.",
            "Étire tes jambes de bois, tu l'as mérité.",
            "Les doublons tintent dans le coffre. Quelle mélodie !",
            "Va boire de l'eau. Pas de l'eau de mer, hein.",
            "Le perroquet fait la sieste. Fais comme lui.",
            "Regarde au loin, matelot. Tes yeux te remercieront.",
            "Pause bien méritée ! Même le kraken prend son goûter.",
            "Range ton sabre cinq minutes, la mer ne va pas s'enfuir.",
            "Une petite danse de pirate ? Personne ne regarde. Sauf moi.",
            "Respire l'air du large... ou celui de ton salon, ça marche aussi.",
            "Le cuisinier du bord a fait des crêpes. Enfin, il a essayé.",
            "Hamac déployé, bottes retirées. Ahhh.",
            "Va saluer les mouettes. Ou ton chat. Même combat.",
            "Les vrais pirates s'étirent. Les faux ont mal au dos.",
            "Repos, matelot ! C'est un ordre du capitaine.",
            "Compte tes doublons. Puis recompte-les. C'est relaxant.",
            "Un biscuit de mer ? Il est dur comme du bois, mais il est à toi.",
            "Pendant la pause, le navire se repose aussi. Il grince de joie.",
            "Écoute le bruit des vagues... ou d'une playlist « bruit des vagues ».",
            "Profite, la prochaine traversée sera épique. Comme toutes les autres.",
        ),
        Phase.SUNK to listOf(
            "Glou glou glou... Le navire a coulé. Les poissons rigolent.",
            "Abandonner le navire ? Même le perroquet est déçu.",
            "Tu as sombré, mais un pirate se relève toujours. Arr.",
        ),
    )

    private val caught = listOf(
        "{app} ? En pleine traversée ? Ah non, moussaillon. Ah non.",
        "Halte ! {app} est une sirène, et tu allais plonger.",
        "Je t'ai vu ouvrir {app}. Le perroquet aussi. Il est déçu.",
        "{app}, c'est le Triangle des Bermudes du temps libre. Demi-tour !",
        "Mille sabords ! Range-moi ce {app} avant que je le jette par-dessus bord.",
        "Tu cherches le trésor dans {app} ? Il n'y est pas. Il est dans ta tâche.",
        "Abordage repoussé ! {app} attendra la fin de la traversée.",
        "Pris la main dans le coffre ! {app} est sous scellés, moussaillon.",
        "{app} ? Je l'ai enfermé dans la cale. Avec les rats.",
        "Tu croyais que je dormais ? Un pirate ne dort que d'un œil. L'autre a un cache.",
        "Alerte au mât ! Un moussaillon tente de déserter vers {app} !",
        "{app} n'a jamais rapporté un seul doublon. Moi si. Réfléchis.",
        "Encore {app} ? La planche est cirée, tu veux l'essayer ?",
        "Hé ! On ne quitte pas le navire pour aller voir des vidéos de chats.",
        "{app} te fait les yeux doux. C'est un piège, matelot, un piège !",
        "Le kraken a avalé {app}. Il le recrachera à la fin de la traversée.",
        "Demi-tour, capitaine de pacotille ! {app} est en eaux interdites.",
        "J'ai jeté {app} dans le Triangle des Bermudes. Désolé, pas désolé.",
        "Même le perroquet sait que {app} peut attendre. Et il a un cerveau de petit pois.",
        "Tentative d'évasion repérée ! Retourne ramer, matelot.",
        "{app} ? Pfff. Les vrais pirates font défiler des cartes au trésor.",
    )

    /** Ce que dit le capitaine quand il te surprend sur une appli interdite. */
    fun caught(appName: String, current: String? = null, random: Random = Random.Default): String {
        val lines = caught.map { it.replace("{app}", appName) }
        return lines.filter { it != current }.ifEmpty { lines }.random(random)
    }

    /** Une réplique au hasard pour la phase, différente de [current] si possible. */
    fun randomFor(phase: Phase, current: String? = null, random: Random = Random.Default): String {
        val pool = quotes.getValue(phase)
        val candidates = pool.filter { it != current }.ifEmpty { pool }
        return candidates.random(random)
    }
}
