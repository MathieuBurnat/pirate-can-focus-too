package dev.mathieuburnat.piratefocus.focus

import kotlin.random.Random

object PirateQuotes {

    private val quotes = mapOf(
        Phase.IDLE to listOf(
            "Moussaillon ! On lève l'ancre ou on bronze sur le quai ?",
            "Un vrai pirate ne scrolle pas. Il navigue.",
            "Le trésor ne va pas se déterrer tout seul, sacrebleu !",
            "Choisis ta traversée, et que Neptune t'épargne les notifs.",
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
        ),
        Phase.SUNK to listOf(
            "Glou glou glou... Le navire a coulé. Les poissons rigolent.",
            "Abandonner le navire ? Même le perroquet est déçu.",
            "Tu as sombré, mais un pirate se relève toujours. Arr.",
        ),
    )

    fun randomFor(phase: Phase, random: Random = Random.Default): String =
        quotes.getValue(phase).random(random)
}
