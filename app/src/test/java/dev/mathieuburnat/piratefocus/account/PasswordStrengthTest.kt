package dev.mathieuburnat.piratefocus.account

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    private fun strength(password: String, email: String = "") = PasswordStrengths.of(password, email)

    @Test
    fun `trop court ou trop connu, c'est faible`() {
        assertEquals(PasswordStrength.WEAK, strength("Ab1!"))
        assertEquals(PasswordStrength.WEAK, strength("motdepasse"))
        assertEquals(PasswordStrength.WEAK, strength("Pirate2026!"))
        assertEquals(PasswordStrength.WEAK, strength("12345678"))
        assertEquals(PasswordStrength.WEAK, strength("aaaaaaaaaa"))
        assertEquals(PasswordStrength.WEAK, strength("abcdefghij"))
    }

    @Test
    fun `le nom de l'email dans le mot de passe, c'est faible`() {
        assertEquals(PasswordStrength.WEAK, strength("annebonny-99", email = "annebonny@flotte.fr"))
    }

    @Test
    fun `plus de variété et de longueur, plus c'est solide`() {
        assertEquals(PasswordStrength.MEDIUM, strength("voilesrouges8"))
        assertEquals(PasswordStrength.STRONG, strength("Voilesrouges8"))
        assertEquals(PasswordStrength.VERY_STRONG, strength("Voiles-Rouges-du-Large-8"))
    }
}
