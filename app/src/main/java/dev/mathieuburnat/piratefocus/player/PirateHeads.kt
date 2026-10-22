package dev.mathieuburnat.piratefocus.player

// Hors de l'objet PirateHeads : les cheveux (PirateHeads.Hair) s'en servent sans attendre que l'objet soit prêt.

/** Le haut du crâne quand il n'y a pas de chapeau (Hat.BALD) mais des cheveux. */
private val LONG_TOP = listOf(
    "................",
    "................",
    "....NNNNNNNN....",
    "..NNNNNNNNNNNN..",
    ".NNNNNNNNNNNNNN.",
    ".NNNNNNNNNNNNNN.",
)
private val SHORT_TOP = listOf(
    "................",
    "................",
    "................",
    "...NNNNNNNNNN...",
    "..NNNNNNNNNNNN..",
    "..NNNNNNNNNNNN..",
)
private val CROP_TOP = listOf(
    "................",
    "................",
    "................",
    ".....NNNNNN.....",
    "...NNNNNNNNNN...",
    "..NNNNNNNNNNNN..",
)

private fun cells(rows: IntRange, vararg columns: Int) = rows.flatMap { row -> columns.map { row to it } }

/**
 * Les têtes de matelots prédéfinies (#18), en grilles de caractères comme les autres sprites (un caractère = une couleur).
 * Chaque tête assemble un chapeau, des yeux, une barbe (ou des cheveux) et des couleurs. Les têtes 0, 50 et 100 sont
 * celles des mousses, au départ ; la tête du capitaine n'est qu'à lui.
 * L'ordre ne doit jamais changer : le numéro d'une tête est gardé sur le téléphone et chez le Worker.
 * Logique pure (couleurs en ARGB), testée.
 */
object PirateHeads {

    const val PER_KIND = 50

    /** Têtes ajoutées ensuite : 20 flibustiers aux cheveux longs, puis 20 flibustières aux cheveux courts. */
    private const val EXTRA = 20

    /** 150 têtes assemblées, 40 de plus (cheveux longs, cheveux courts), puis les têtes dessinées à la main. */
    val COUNT get() = all.size

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

    /**
     * Pirate garçon, pirate fille, ou pirate neutre (plus androgyne) : 50 têtes chacun.
     * À l'écran, des noms de pirates : flibustier, flibustière, et corsaire (le même mot pour tout le monde).
     */
    enum class Kind(val label: String) {
        GARCON("FLIBUSTIER"),
        FILLE("FLIBUSTIÈRE"),
        ANDROGYNE("CORSAIRE"),
    }

    /** Les cheveux : [top] sans chapeau, et les mèches ([cells], ligne et colonne) posées là où il n'y a rien. */
    enum class Hair(val label: String, val top: List<String>?, val cells: List<Pair<Int, Int>>) {
        NONE("", null, emptyList()),
        LONG("cheveux longs", LONG_TOP, cells(5..15, 1, 14) + cells(8..15, 0, 15)),
        BRAID("tresse", LONG_TOP, cells(5..11, 1, 14) + cells(12..15, 14, 15) + cells(9..11, 15)),
        BOB("carré", LONG_TOP, cells(5..12, 0, 1, 14, 15)),
        SHORT("cheveux courts", SHORT_TOP, cells(5..8, 1, 14)),
        CROP("tondu", CROP_TOP, emptyList()),
    }

    /** Le bas du visage d'une pirate fille (lèvres et joues roses) et d'un pirate qui ne dit rien. */
    private val LIPS = listOf("..SCSSSLLSSSCS..", "..SSSSSSSSSSSS..", "...SSSSSSSSSS...", "....SSSSSSSS....", "................")
    private val NEUTRAL = listOf("..SSSSSSSSSSSS..", "..SSSSMMMMSSSS..", "...SSSSSSSSSS...", "....SSSSSSSS....", "................")

    /** Des cils au-dessus de chaque œil ouvert. */
    private fun lashes(above: String, eyes: String): String =
        above.mapIndexed { i, c -> if (eyes[i] == 'E' && c == 'S') 'E' else c }.joinToString("")

    val SKINS = listOf(0xFFE8B98AL, 0xFFC68B59L, 0xFF8D5524L)
    val BEARDS = listOf(0xFF7A4A21L, 0xFF2A2A2AL, 0xFFC1440EL, 0xFF9E9E9EL)
    val HAIRS = listOf(0xFF6B3E1FL, 0xFF1E1E1EL, 0xFFB5371BL, 0xFFE6C35CL)
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
        'C' to 0xFFF28B82L, // joues roses
        'L' to 0xFFC2185BL, // lèvres
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
        val kind: Kind = Kind.GARCON,
        /** Les cheveux (les garçons n'en ont que dans les têtes ajoutées ensuite) ; leur couleur est HAIRS[beardColor]. */
        val hair: Hair = Hair.NONE,
        /** Une tête dessinée à la main (Sparrow, Miku...) : elle remplace tout l'assemblage. */
        val special: Special? = null,
    ) {
        /** 16 × 16 caractères, '.' = transparent. */
        val sprite: List<String>
            get() {
                special?.let { return it.rows }
                val eyeRows = eyes.rows.mapIndexed { i, row -> if (earring && i > 0) row.dropLast(1) + "G" else row }
                val top = if (hat == Hat.BALD) hair.top ?: hat.rows else hat.rows
                val face = when (kind) {
                    Kind.GARCON -> top + FACE + eyeRows + FACE + beard.rows
                    Kind.FILLE -> top + FACE + listOf(lashes(eyeRows[0], eyeRows[1])) + eyeRows.drop(1) + FACE + LIPS
                    Kind.ANDROGYNE -> top + FACE + eyeRows + FACE + NEUTRAL
                }
                val rows = face.toMutableList()
                hair.cells.forEach { (row, column) ->
                    if (rows[row][column] == '.') rows[row] = rows[row].replaceRange(column, column + 1, "N")
                }
                return rows
            }

        /** Couleur ARGB de chaque caractère du sprite. */
        val colors: Map<Char, Long>
            get() = special?.colors ?: (
                FIXED + mapOf(
                    'S' to SKINS[skin],
                    'B' to BEARDS[beardColor],
                    'N' to HAIRS[beardColor],
                    'K' to HATS[hatColor],
                    'R' to BANDS[band],
                )
                )

        val label: String
            get() = special?.name ?: listOfNotNull(
                hat.label,
                eyes.label,
                beard.label.takeIf { kind == Kind.GARCON },
                hair.label.takeIf { hair != Hair.NONE },
            ).joinToString(", ")

        /** Ce qu'on voit vraiment : deux têtes au même [look] se ressemblent comme deux gouttes de rhum. */
        val look: Pair<List<String>, Map<Char, Long>>
            get() {
                val sprite = sprite
                val used = sprite.joinToString("").toSet()
                return sprite to colors.filterKeys { it in used }
            }
    }

    /** La tête du capitaine : elle n'est qu'à lui, aucun matelot ne peut la porter. */
    private val captain = Head(-1, Hat.TRICORNE_SKULL, Eyes.PATCH_LEFT, Beard.FULL, 0, 0, 0, 0, earring = true)

    /** Les têtes de départ : des mousses au bandana bleu, tout juste montés à bord (numéros 0, 50 et 100). */
    private val cabinBoy = Head(0, Hat.BANDANA, Eyes.TWO_EYES, Beard.SHAVED, 0, 0, 0, 1, earring = false)
    private val cabinGirl = Head(PER_KIND, Hat.BANDANA, Eyes.TWO_EYES, Beard.SHAVED, 0, 0, 0, 1, false, Kind.FILLE, Hair.BOB)
    private val cabinSecret = Head(2 * PER_KIND, Hat.BANDANA, Eyes.TWO_EYES, Beard.SHAVED, 0, 1, 0, 1, false, Kind.ANDROGYNE, Hair.SHORT)

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


    /** Les combinaisons d'une pirate fille ou d'un pirate qui ne dit rien : des cheveux au lieu de la barbe. */
    private fun combination(index: Int, id: Int, kind: Kind, hairs: List<Hair>): Head {
        var rest = index
        fun next(radix: Int): Int = (rest % radix).also { rest /= radix }
        return Head(
            id = id,
            hat = Hat.entries[next(Hat.entries.size)],
            eyes = Eyes.entries[next(Eyes.entries.size)],
            beard = Beard.SHAVED,
            hair = hairs[next(hairs.size)],
            skin = next(SKINS.size),
            beardColor = next(HAIRS.size),
            hatColor = next(HATS.size),
            band = next(BANDS.size),
            earring = next(2) == 1,
            kind = kind,
        )
    }

    /**
     * Tirage figé (générateur congruentiel maison, jamais celui de Kotlin qui peut changer d'une version à l'autre) :
     * [PER_KIND] têtes à partir de [starter], toutes différentes.
     */
    private fun draw(starter: Head, seed: Long, combinations: Int, make: (index: Int, id: Int) -> Head): List<Head> = buildList {
        add(starter)
        var state = seed
        while (size < PER_KIND) {
            state = (state * 1_103_515_245L + 12_345L) and 0x7FFF_FFFFL
            val candidate = make(((state shr 8) % combinations).toInt(), starter.id + size)
            if (candidate.look != captain.look && none { it.look == candidate.look }) add(candidate)
        }
    }

    private val GIRL_HAIRS = listOf(Hair.LONG, Hair.BRAID, Hair.BOB)
    private val SECRET_HAIRS = listOf(Hair.SHORT, Hair.CROP)
    private fun combinations(hairs: Int) = Hat.entries.size * Eyes.entries.size * hairs * SKINS.size * HAIRS.size * HATS.size * BANDS.size * 2

    /** [count] têtes de plus, à partir du numéro qui suit [before], différentes de toutes celles déjà là. */
    private fun drawMore(before: List<Head>, count: Int, seed: Long, combinations: Int, make: (index: Int, id: Int) -> Head): List<Head> {
        val drawn = mutableListOf<Head>()
        var state = seed
        while (drawn.size < count) {
            state = (state * 1_103_515_245L + 12_345L) and 0x7FFF_FFFFL
            val candidate = make(((state shr 8) % combinations).toInt(), before.size + drawn.size)
            if (candidate.look != captain.look && (before + drawn).none { it.look == candidate.look }) drawn += candidate
        }
        return drawn
    }

    /** Les flibustiers aux cheveux longs (coucou Sparrow) : une barbe et des cheveux. */
    private fun longHairedBoy(index: Int, id: Int): Head {
        var rest = index
        fun next(radix: Int): Int = (rest % radix).also { rest /= radix }
        return Head(
            id = id,
            hat = Hat.entries[next(Hat.entries.size)],
            eyes = Eyes.entries[next(Eyes.entries.size)],
            beard = Beard.entries[next(Beard.entries.size)],
            hair = GIRL_HAIRS[next(GIRL_HAIRS.size)],
            skin = next(SKINS.size),
            beardColor = next(BEARDS.size),
            hatColor = next(HATS.size),
            band = next(BANDS.size),
            earring = next(2) == 1,
        )
    }

    /** Numéros 0 à 49 : flibustiers ; 50 à 99 : flibustières ; 100 à 149 : corsaires (neutres). */
    private val first150: List<Head> =
        draw(cabinBoy, 1_717L, COMBINATIONS, ::combination) +
            draw(cabinGirl, 2_024L, combinations(GIRL_HAIRS.size)) { index, id -> combination(index, id, Kind.FILLE, GIRL_HAIRS) } +
            draw(cabinSecret, 1_337L, combinations(SECRET_HAIRS.size)) { index, id -> combination(index, id, Kind.ANDROGYNE, SECRET_HAIRS) }

    /** Numéros 150 à 169 : flibustiers aux cheveux longs. */
    private val longHairedBoys = drawMore(first150, EXTRA, 4_242L, COMBINATIONS * GIRL_HAIRS.size, ::longHairedBoy)

    /** Numéros 170 à 189 : flibustières aux cheveux courts. */
    private val shortHairedGirls = drawMore(first150 + longHairedBoys, EXTRA, 7_777L, combinations(SECRET_HAIRS.size)) { index, id ->
        combination(index, id, Kind.FILLE, SECRET_HAIRS)
    }

    /**
     * Toutes les têtes. Les nouvelles s'ajoutent toujours à la fin : 190 et suivantes, les têtes dessinées à la main
     * ([SPECIALS]).
     */
    val all: List<Head> = (first150 + longHairedBoys + shortHairedGirls).let { drawn ->
        drawn + SPECIALS.mapIndexed { i, special ->
            Head(drawn.size + i, Hat.BALD, Eyes.TWO_EYES, Beard.SHAVED, 0, 0, 0, 0, false, special.kind, special = special)
        }
    }

    /** Les têtes de départ, toujours permises (une par sorte de pirate). */
    val STARTERS = setOf(0, PER_KIND, 2 * PER_KIND)

    fun ofKind(kind: Kind): List<Head> = all.filter { it.kind == kind }

    /** Une tête inconnue (Worker plus récent que l'app) : celle du mousse. */
    operator fun get(id: Int): Head = all.getOrElse(id) { cabinBoy }
}

/** Une tête dessinée à la main, pixel par pixel, avec ses propres couleurs (ARGB). */
class Special(val name: String, val kind: PirateHeads.Kind, val rows: List<String>, val colors: Map<Char, Long>)

/** Les têtes dessinées à la main, à la fin du catalogue (190 et suivantes). Toujours en ajouter à la fin ! */
private val SPECIALS = listOf(
    Special(
        "Capitaine Sparrow",
        PirateHeads.Kind.GARCON,
        listOf(
            "................",
            "................",
            "....RRRRRRRR....",
            "..RRRRRRRRRRRR..",
            ".RRRRRRRRRRRRRRR",
            ".NRRRRRRRRRRRRNR",
            ".NSSSSSSSSSSSSN.",
            "NNSKKSSSSSSKKSNN",
            "NNSEESSSSSSEESNN",
            "GNSSSSSSSSSSSSNN",
            "NNSSSSSSSSSSSSNN",
            "NNSBBBMMMMBBBSNN",
            "NNSSSSSSSSSSSSNG",
            "GN.SSSSBBSSSS.NN",
            "NN..SSSBBSSS..GN",
            "N......BB.....N.",
        ),
        mapOf(
            'R' to 0xFFD64545L, 'N' to 0xFF3B2414L, 'S' to 0xFFC68B59L, 'K' to 0xFF1A1A1AL,
            'E' to 0xFF2B2B2BL, 'B' to 0xFF3B2414L, 'M' to 0xFF8C2F2FL, 'G' to 0xFFF4C542L,
        ),
    ),
    Special(
        "Goth de la Tortuga",
        PirateHeads.Kind.FILLE,
        listOf(
            "......KKKK......",
            "....KKKKKKKK....",
            "..KKKKKWWKKKKK..",
            ".KKKKKWKKWKKKKK.",
            "KKKKKKKWWKKKKKKK",
            ".NRRRRRRRRRRRRN.",
            ".NSSSSSSSSSSSSN.",
            "NNSDDSSSSSSDDSNN",
            "NNSEESSSSSSEESNN",
            "NNSSSSSSSSSSSSNN",
            "NNSSSSSSSSSSSSNN",
            "NNSSSSSLLSSSSSNN",
            "NNNSSSSSSSSSSNNN",
            "NNN.SSSSSSSS.NNN",
            "NNN...SSSS...NNN",
            "NN............NN",
        ),
        mapOf(
            'K' to 0xFF1B1B1BL, 'W' to 0xFFF2E8CFL, 'R' to 0xFF6A1B9AL, 'N' to 0xFF0D0D0DL,
            'S' to 0xFFF7F1ECL, 'D' to 0xFF4A148CL, 'E' to 0xFF111111L, 'L' to 0xFF6A1B9AL,
        ),
    ),
    Special(
        "Miku des Mers",
        PirateHeads.Kind.ANDROGYNE,
        listOf(
            "................",
            "....NNNNNNNN....",
            "..NNNNNNNNNNNN..",
            ".NNNNNNNNNNNNNN.",
            "PNNNNNNNNNNNNNNP",
            "PNNNNSSSSSSNNNNP",
            "NNNSSSSSSSSSSNNN",
            "NNSSSSSSSSSSSSNN",
            "NNSSEESSSSEESSNN",
            "NNSSEESSSSEESSNN",
            "NNSCSSSSSSSSCSNN",
            "NNSSSSSMMSSSSSNN",
            "NN.SSSSSSSSSS.NN",
            "NN..SSSSSSSS..NN",
            "NN............NN",
            "NN............NN",
        ),
        mapOf(
            'N' to 0xFF39C5BBL, 'P' to 0xFFE8467CL, 'S' to 0xFFF5D6C0L, 'E' to 0xFF1E8C84L,
            'C' to 0xFFF28B82L, 'M' to 0xFFC2185BL,
        ),
    ),
    Special(
        "Emo du Kraken",
        PirateHeads.Kind.ANDROGYNE,
        listOf(
            "................",
            "....NNNNNNNN....",
            "..NNNNNNNNNQQN..",
            ".NNNNNNNNNNNNNN.",
            ".NNNNNNNNNNNNNN.",
            ".NNNNNNNNNSSSNN.",
            ".NNNNNNNNSSSSSN.",
            ".NNNNNNNSSSSSSN.",
            ".NNNNNNSSSKEESN.",
            ".NNNNNSSSSSSSSN.",
            "..NNNSSSSSSSSS..",
            "..NNSSSSMMMSSS..",
            "..NSSSSSSGSSSS..",
            "...SSSSSSSSSS...",
            "....SSSSSSSS....",
            "................",
        ),
        mapOf(
            'N' to 0xFF151515L, 'Q' to 0xFFE91E63L, 'S' to 0xFFF2E6DAL, 'K' to 0xFF000000L,
            'E' to 0xFF6A1B9AL, 'M' to 0xFF4A4A4AL, 'G' to 0xFFC0C0C0L,
        ),
    ),
    Special(
        "Punk des Caraïbes",
        PirateHeads.Kind.ANDROGYNE,
        listOf(
            ".......QQ.......",
            "......QQQQ......",
            "......QQQQ......",
            ".....QQQQQQ.....",
            "....SSQQQQSS....",
            "..SSSSQQQQSSSS..",
            "..SSSSSSSSSSSS..",
            "..SSSSSSSSSSSS..",
            "..SSEESSSSEESSG.",
            "..SSSSSSSSSSSSG.",
            "..SSFSSSSSSFSS..",
            "..SSSSMMMMSSSS..",
            "..SSSSSGSSSSSS..",
            "...SSSSSSSSSS...",
            "....SSSSSSSS....",
            "................",
        ),
        mapOf(
            'Q' to 0xFFFF4FA3L, 'S' to 0xFFE0AC80L, 'E' to 0xFF2B2B2BL, 'G' to 0xFFC0C0C0L,
            'F' to 0xFFB5774AL, 'M' to 0xFF8C2F2FL,
        ),
    ),
    Special(
        "Fantôme du Hollandais",
        PirateHeads.Kind.ANDROGYNE,
        listOf(
            "......KKKK......",
            "....KKKKKKKK....",
            "..KKKKKKKKKKKK..",
            ".KKKK.KKKK.KKKK.",
            "KKKKKKKKKKKKKKKK",
            "..RRRRRRRRRRRR..",
            "..SSSSSSSSSSSS..",
            "..SSSSSSSSSSSS..",
            "..SEESSSSSSEES..",
            "..SEESSSSSSEES..",
            "..SSSSSSSSSSSS..",
            "..SSSMMMMMMSSS..",
            "..BSSSSSSSSSSB..",
            "...BBSSSSSSBB...",
            "....BBBBBBBB....",
            ".....B.BB.B.....",
        ),
        mapOf(
            'K' to 0xFF2F4F4FL, 'R' to 0xFF4F7F7FL, 'S' to 0xFF9FE0D8L, 'E' to 0xFF0B3B3BL,
            'M' to 0xFF0B3B3BL, 'B' to 0xFF3E7D5AL,
        ),
    ),
)
