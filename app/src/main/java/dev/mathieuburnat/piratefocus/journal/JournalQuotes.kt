package dev.mathieuburnat.piratefocus.journal

import kotlin.random.Random

object JournalQuotes {

    /** Ce que dit le capitaine pendant qu'on tapote sur le journal verrouillé (tap 1 à 6). */
    fun knock(tapsLeft: Int): String? = when (tapsLeft) {
        4 -> "Toc toc ? Ce journal est fermé, moussaillon."
        3 -> "J'ai dit FERMÉ. Tu es sourd comme un hareng ?"
        2 -> "Encore deux coups et j'appelle le kraken..."
        1 -> "Bon. Un dernier coup, et je craque."
        else -> null
    }

    const val UNLOCKED = "C'est bon, c'est bon, je te laisse entrer ! Mais attention : c'est encore en travaux. " +
        "Ne marche pas sur la peinture fraîche."

    private val verdicts = mapOf(
        Verdict.PAGE_BLANCHE to listOf(
            "Page blanche. Tu n'as rien fait, ou tu as tout oublié ?",
            "Un journal vide, c'est comme une cale sans rhum : triste.",
            "Note quelque chose, moule à marée basse ! Une pompe, une pinte, n'importe quoi !",
        ),
        Verdict.ATHLETE to listOf(
            "Que du sport et pas une goutte ? Tu es un pirate ou un moine ?",
            "Regardez-moi ces biceps ! On dirait le mât de misaine.",
            "Tant de muscles... Tu vas finir par soulever le navire, sardine.",
            "Pas un verre ? Même le perroquet trouve ça louche.",
        ),
        Verdict.EPONGE to listOf(
            "Zéro séance et que des verres ? Éponge de cale, va !",
            "Tu n'as pas fait une pompe, mais tu as pompé la réserve. Bravo, outre à rhum.",
            "Le seul muscle que tu as travaillé, c'est le coude. Mollusque !",
            "À ce rythme, on va te ranger avec les tonneaux, barrique ambulante.",
        ),
        Verdict.PILIER_DE_TAVERNE to listOf(
            "Plus de verres que de séances... Pilier de taverne, va !",
            "Les bouteilles mènent au score, crabe ramolli. Va grimper un mât !",
            "Ton foie réclame une mutinerie, et je le soutiens.",
            "Tu bois plus vite que tu ne rames, sardine dessalée.",
        ),
        Verdict.EQUILIBRE to listOf(
            "Égalité parfaite ! Une pompe, une pinte. La voie du vrai pirate.",
            "Match nul entre tes muscles et ton gosier. Le capitaine approuve.",
            "Équilibre parfait... pour l'instant. Je te surveille, moussaillon.",
        ),
        Verdict.SPORTIF to listOf(
            "Les muscles mènent ! Tu peux t'offrir un petit verre, champion.",
            "Plus de sport que de rhum ? Tu deviens raisonnable, ça m'inquiète.",
            "Pas mal, pas mal. Tu pourrais presque porter le coffre au trésor tout seul.",
        ),
    )

    private val reactions = mapOf(
        Entry.MEGA_SEANCE to listOf(
            "Méga séance ! Les tractions, c'est comme hisser les voiles.",
            "Des pompes ! Le pont tremble sous ta puissance.",
            "Hissez ! Ho ! Encore une traction !",
        ),
        Entry.GRIMPE to listOf(
            "Tu grimpes comme un mousse dans les haubans !",
            "Grimpe validée. La vigie a une place pour toi.",
            "Attention au vertige, petit singe des mers.",
        ),
        Entry.ABDOS to listOf(
            "Des abdos en béton, comme une coque toute neuve.",
            "P'tite séance, mais séance quand même. Arr !",
            "Gainage ! Tu tiens mieux que la grand-voile.",
        ),
        Entry.BIERE to listOf(
            "🍺 Une pinte ! Santé, matelot !",
            "🍺 La mousse, c'est pour le pont. Pas pour la moustache.",
            "🍺 Encore une bière ? Les tonneaux commencent à avoir peur.",
            "🍺 Glou glou glou... comme le navire quand il coule.",
        ),
        Entry.COCKTAIL to listOf(
            "🍹 Un cocktail ? Avec une petite ombrelle ? Très pirate, ça.",
            "🍹 Mojito, piña colada... Tu te crois en croisière ?",
            "🍹 Le capitaine tolère l'ombrelle. Pour cette fois.",
            "🍹 Un cocktail, c'est du rhum déguisé. Je valide.",
        ),
        Entry.VIN to listOf(
            "🍷 Du vin ! Tu te prends pour un amiral, maintenant ?",
            "🍷 Un petit verre de rouge pour le scorbut, c'est médical.",
            "🍷 Le vin, ça se boit avec le petit doigt levé. Même la main crochet.",
            "🍷 Château Barbe-Noire, grand cru de la cale. Excellent choix.",
        ),
    )

    /** Les grandes interventions du capitaine, quand les verres s'accumulent. */
    private val interventions = listOf(
        "INTERVENTION DU CAPITAINE ! {n} verres. Je confisque la clé de la cale.",
        "Ça fait {n} verres, mon gaillard. Le perroquet appelle ta mère.",
        "{n} verres ?! Même le kraken a demandé un verre d'eau.",
        "Au bout de {n} verres, on ne marche plus droit, on navigue. Fais une pompe pour voir.",
    )

    fun verdict(verdict: Verdict, current: String? = null, random: Random = Random.Default): String {
        val pool = verdicts.getValue(verdict)
        return pool.filter { it != current }.ifEmpty { pool }.random(random)
    }

    fun reaction(entry: Entry, random: Random = Random.Default): String = reactions.getValue(entry).random(random)

    /** Tous les 5 verres, le capitaine intervient en personne. */
    fun intervention(drinks: Int, random: Random = Random.Default): String? =
        if (drinks > 0 && drinks % 5 == 0) interventions.random(random).replace("{n}", "$drinks") else null
}
