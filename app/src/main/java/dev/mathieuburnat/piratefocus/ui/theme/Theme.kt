package dev.mathieuburnat.piratefocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val SeaNight = Color(0xFF0B1A2E)
val SeaDeep = Color(0xFF14304F)
val Foam = Color(0xFF7FB7D9)
val Parchment = Color(0xFFF2E8CF)
val Doubloon = Color(0xFFF4C542)
val PirateRed = Color(0xFFD64545)
/** Couleur des traversées de focus dans les statistiques. */
val Lagoon = Color(0xFF6FD3A8)

private val PirateColors = darkColorScheme(
    primary = Doubloon,
    onPrimary = SeaNight,
    secondary = Foam,
    onSecondary = SeaNight,
    error = PirateRed,
    background = SeaNight,
    onBackground = Parchment,
    surface = SeaDeep,
    onSurface = Parchment,
)

/** Toute la typo en monospace : c'est la marque de fabrique du capitaine. */
private val PirateTypography = Typography().run {
    fun TextStyle.mono() = copy(fontFamily = FontFamily.Monospace)
    Typography(
        displayLarge = displayLarge.mono(),
        displayMedium = displayMedium.mono(),
        displaySmall = displaySmall.mono(),
        headlineLarge = headlineLarge.mono(),
        headlineMedium = headlineMedium.mono(),
        headlineSmall = headlineSmall.mono(),
        titleLarge = titleLarge.mono(),
        titleMedium = titleMedium.mono(),
        titleSmall = titleSmall.mono(),
        bodyLarge = bodyLarge.mono(),
        bodyMedium = bodyMedium.mono(),
        bodySmall = bodySmall.mono(),
        labelLarge = labelLarge.mono(),
        labelMedium = labelMedium.mono(),
        labelSmall = labelSmall.mono(),
    )
}

@Composable
fun PirateFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PirateColors, typography = PirateTypography, content = content)
}
