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

    /** Une réplique au hasard pour la phase, différente de [current] si possible. */
    fun randomFor(phase: Phase, current: String? = null, random: Random = Random.Default): String {
        val pool = quotes.getValue(phase)
        val candidates = pool.filter { it != current }.ifEmpty { pool }
        return candidates.random(random)
    }
}
