package dev.mathieuburnat.piratefocus.account

/** La solidité d'un mot de passe, du radeau au coffre-fort (indicatif : seul le minimum de 8 caractères bloque). */
enum class PasswordStrength { WEAK, MEDIUM, STRONG, VERY_STRONG }

/** Évalue un mot de passe : longueur, variété des caractères, et mots de passe trop connus (logique pure, testée). */
object PasswordStrengths {

    /** Les mots de passe que même un mousse devinerait. */
    private val COMMON = listOf(
        "password", "motdepasse", "123456", "12345678", "123456789", "azerty", "qwerty", "qwertz",
        "pirate", "capitaine", "soleil", "bonjour", "admin", "letmein", "000000", "111111", "abc123",
    )

    private val SEQUENCES = listOf("0123456789", "abcdefghijklmnopqrstuvwxyz", "azertyuiop", "qwertyuiop", "qwertzuiop")

    fun of(password: String, email: String = ""): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        val lower = password.lowercase()
        val emailName = email.substringBefore('@').lowercase()
        val guessable = COMMON.any { it in lower } ||
            (emailName.length >= 3 && emailName in lower) ||
            isSequenceOrRepeat(lower)
        if (guessable) return PasswordStrength.WEAK

        val variety = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        var score = variety
        if (password.length >= 12) score++
        if (password.length >= 16) score++
        return when {
            score >= 5 -> PasswordStrength.VERY_STRONG
            score >= 4 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    /** « aaaaaaaa », « 12345678 », « abcdefgh », « azertyui »... */
    private fun isSequenceOrRepeat(lower: String): Boolean =
        lower.toSet().size <= 2 || SEQUENCES.any { seq -> seq.contains(lower) || seq.reversed().contains(lower) }
}
