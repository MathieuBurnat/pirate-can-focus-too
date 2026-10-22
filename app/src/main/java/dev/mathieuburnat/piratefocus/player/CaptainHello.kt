package dev.mathieuburnat.piratefocus.player

import kotlin.random.Random

/** Au menu, le capitaine salue le matelot par son pseudo environ une fois sur [EVERY] (#17 ; logique pure, testée). */
object CaptainHello {

    const val EVERY = 5

    private val greetings = listOf(
        "Ahoy, %s !",
        "Ahoy, %s ! Prêt à lever l'ancre ?",
        "Tiens, %s ! Le navire t'attendait.",
        "Salut à toi, %s, loup de mer !",
    )

    fun shouldGreet(pirateName: String, random: Random = Random.Default): Boolean =
        pirateName.isNotBlank() && random.nextInt(EVERY) == 0

    fun greeting(pirateName: String, random: Random = Random.Default): String = greetings.random(random).format(pirateName)
}
