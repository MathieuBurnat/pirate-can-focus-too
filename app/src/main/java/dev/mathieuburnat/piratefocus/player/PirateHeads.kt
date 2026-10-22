package dev.mathieuburnat.piratefocus.player

/**
 * Les têtes de matelots prédéfinies (#18), en grilles de caractères comme les autres sprites (un caractère = une couleur).
 * Chaque tête assemble un chapeau, des yeux, une barbe et des couleurs. La tête 0 est celle du capitaine.
 * L'ordre ne doit jamais changer : le numéro d'une tête est gardé sur le téléphone et chez le Worker.
 * Logique pure (couleurs en ARGB), testée.
 */
object PirateHeads {

    const val COUNT = 50

    enum class Hat(val label: String, val rows: List<String>) {
        TRICORNE_SKULL(
            "tricorne",
            listOf(
                "......KKKK......",
                "....KKKKKKKK....",
                "..KKKKKWWKKKKK..",
                ".KKKKKWKKWKKKKK.",
                "KKKKKKKWWKKKKKKK",
                "..RRRRRRRRRRRR..",
            ),
        ),
        TRICORNE(
            "tricorne sobre",
            listOf(
                "......KKKK......",
                "....KKKKKKKK....",
                "..KKKKKKKKKKKK..",
                ".KKKKKKKKKKKKKK.",
                "KKKKKKKKKKKKKKKK",
                "..RRRRRRRRRRRR..",
            ),
        ),
        FEATHER(
            "chapeau à plume",
            listOf(
                "......KKKK.QQQ..",
                "....KKKKKKQQ....",
                "..KKKKKKKKQKKK..",
                ".KKKKKKKKKKKKKK.",
                "KKKKKKKKKKKKKKKK",
                "..RRRRRRRRRRRR..",
            ),
        ),
        BICORNE(
            "bicorne",
            listOf(
                "................",
                "................",
                ".KK..........KK.",
                ".KKKKKKKKKKKKKK.",
                "KKKKKKKGGKKKKKKK",
                "..RRRRRRRRRRRR..",
            ),
        ),
        BANDANA(
            "bandana",
            listOf(
                "................",
                "................",
                "....RRRRRRRR....",
                "..RRRRRRRRRRRR..",
                "..RRRRRRRRRRRRRR",
                "..RRRRRRRRRRRR.R",
            ),
        ),
        BALD(
            "crâne lisse",
            listOf(
                "................",
                "................",
                "................",
                ".....SSSSSS.....",
                "...SSSSSSSSSS...",
                "..SSSSSSSSSSSS..",
            ),
        ),
    }

    enum class Eyes(val label: String, val rows: List<String>) {
        PATCH_LEFT("cache-œil", listOf("..SPPPPSSSSSSS..", "..SPPPSSSSEESS..", "..SSPSSSSSSSSS..")),
        PATCH_RIGHT("cache-œil à droite", listOf("..SSSSSSSPPPPS..", "..SSEESSSSPPPS..", "..SSSSSSSSSPSS..")),
        TWO_EYES("deux yeux", listOf("..SSSSSSSSSSSS..", "..SSEESSSSEESS..", "..SSSSSSSSSSSS..")),
        SCAR("balafre", listOf("..SSSMSSSSSSSS..", "..SSEMSSSSEESS..", "..SSSSMSSSSSSS..")),
    }

    enum class Beard(val label: String, val rows: List<String>) {
        FULL(
            "barbe",
            listOf("..BSSSSMMMSSSB..", "..BBSSSSSSSSBB..", "...BBBBBBBBBB...", "....BBBBBBBB....", ".....BBBBBB....."),
        ),
        GOATEE(
            "bouc",
            listOf("..SSSSSMMMSSSS..", "..SSSSSSSSSSSS..", "...SSSSBBSSSS...", "....SSSBBSSS....", "......BBBB......"),
        ),
        MOUSTACHE(
            "moustache",
            listOf("..SSBBBBBBBBSS..", "..SSSSSMMSSSSS..", "...SSSSSSSSSS...", "....SSSSSSSS....", "................"),
        ),
        SHAVED(
            "rasé de près",
            listOf("..SSSMSSSSMSSS..", "..SSSSMMMMSSSS..", "...SSSSSSSSSS...", "....SSSSSSSS....", "................"),
        ),
    }

    val SKINS = listOf(0xFFE8B98AL, 0xFFC68B59L, 0xFF8D5524L)
    val BEARDS = listOf(0xFF7A4A21L, 0xFF2A2A2AL, 0xFFC1440EL, 0xFF9E9E9EL)
    val HATS = listOf(0xFF1B1B1BL, 0xFF1F3A5FL, 0xFF5A1E2BL)
    val BANDS = listOf(0xFFD64545L, 0xFF3F7FD9L, 0xFF3E8E41L, 0xFF7B4FA0L)

    /** Les couleurs qui ne changent pas d'une tête à l'autre. */
    private val FIXED = mapOf(
        'W' to 0xFFF2E8CFL, // tête de mort du chapeau
        'P' to 0xFF111111L, // cache-œil
        'E' to 0xFF2B2B2BL, // œil
        'M' to 0xFF8C2F2FL, // bouche, balafre
        'G' to 0xFFF4C542L, // or : boucle d'oreille, cocarde
        'Q' to 0xFFE53935L, // plume
    )

    private const val FACE = "..SSSSSSSSSSSS.."

    data class Head(
        val id: Int,
        val hat: Hat,
        val eyes: Eyes,
        val beard: Beard,
        val skin: Int,
        val beardColor: Int,
        val hatColor: Int,
        val band: Int,
        val earring: Boolean,
    ) {
        /** 16 × 16 caractères, '.' = transparent. */
        val sprite: List<String>
            get() {
                val eyeRows = eyes.rows.mapIndexed { i, row -> if (earring && i > 0) row.dropLast(1) + "G" else row }
                return hat.rows + FACE + eyeRows + FACE + beard.rows
            }

        /** Couleur ARGB de chaque caractère du sprite. */
        val colors: Map<Char, Long>
            get() = FIXED + mapOf(
                'S' to SKINS[skin],
                'B' to BEARDS[beardColor],
                'K' to HATS[hatColor],
                'R' to BANDS[band],
            )

        val label: String get() = "${hat.label}, ${eyes.label}, ${beard.label}"

        /** Ce qu'on voit vraiment : deux têtes au même [look] se ressemblent comme deux gouttes de rhum. */
        val look: Pair<List<String>, Map<Char, Long>>
            get() {
                val sprite = sprite
                val used = sprite.joinToString("").toSet()
                return sprite to colors.filterKeys { it in used }
            }
    }

    /** La tête du capitaine, celle de départ de tout matelot. */
    private val captain = Head(0, Hat.TRICORNE_SKULL, Eyes.PATCH_LEFT, Beard.FULL, 0, 0, 0, 0, earring = true)

    /** Toutes les combinaisons possibles, dans un ordre fixe. */
    private fun combination(index: Int, id: Int): Head {
        var rest = index
        fun next(radix: Int): Int = (rest % radix).also { rest /= radix }
        return Head(
            id = id,
            hat = Hat.entries[next(Hat.entries.size)],
            eyes = Eyes.entries[next(Eyes.entries.size)],
            beard = Beard.entries[next(Beard.entries.size)],
            skin = next(SKINS.size),
            beardColor = next(BEARDS.size),
            hatColor = next(HATS.size),
            band = next(BANDS.size),
            earring = next(2) == 1,
        )
    }

    private val COMBINATIONS = Hat.entries.size * Eyes.entries.size * Beard.entries.size *
        SKINS.size * BEARDS.size * HATS.size * BANDS.size * 2

    /** Tirage figé (générateur congruentiel maison, jamais celui de Kotlin qui peut changer d'une version à l'autre). */
    val all: List<Head> = buildList {
        add(captain)
        var seed = 1_717L
        while (size < COUNT) {
            seed = (seed * 1_103_515_245L + 12_345L) and 0x7FFF_FFFFL
            val candidate = combination(((seed shr 8) % COMBINATIONS).toInt(), id = size)
            if (none { it.look == candidate.look }) add(candidate)
        }
    }

    /** Une tête inconnue (Worker plus récent que l'app) : celle du capitaine. */
    operator fun get(id: Int): Head = all.getOrElse(id) { captain }
}
