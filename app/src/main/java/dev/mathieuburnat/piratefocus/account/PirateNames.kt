package dev.mathieuburnat.piratefocus.account

import kotlin.random.Random

/** Les pseudos de pirate tirés au sort (les mêmes que côté Worker). Logique pure, testée. */
object PirateNames {

    private val TITLES = listOf("Capitaine", "Matelot", "Moussaillon", "Quartier-maître", "Boucanier", "Corsaire", "Flibustier", "Cuistot")
    private val NAMES = listOf("Jack", "Anne", "Barnabé", "Gaston", "Rosalie", "Hector", "Morgane", "Fernand", "Lucette", "Octave", "Mireille", "Bertrand")
    private val NICKNAMES = listOf(
        "le Borgne", "Jambe-de-Bois", "Barbe-Rousse", "Sans-Dents", "la Sardine", "la Tempête",
        "Crochet-Rouillé", "Mal-Rasé", "Pied-Marin", "Rhum-Arrangé", "Coque-Percée", "Mange-Bigorneaux",
    )

    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 48

    fun random(random: Random = Random.Default): String =
        "${TITLES.random(random)} ${NAMES.random(random)} ${NICKNAMES.random(random)}"

    /** Un pseudo choisi : espaces superflus retirés, 2 à 48 caractères. Null s'il ne convient pas. */
    fun clean(name: String): String? =
        name.replace(Regex("\\s+"), " ").trim().takeIf { it.length in MIN_LENGTH..MAX_LENGTH }
}
