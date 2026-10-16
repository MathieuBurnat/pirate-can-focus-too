package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Un œil grand ouvert : le mot de passe est caché, le toucher le montre. */
private val openEye = listOf(
    ".........",
    "..WWWWW..",
    ".WWAAAWW.",
    "WWAAPAAWW",
    ".WWAAAWW.",
    "..WWWWW..",
    ".........",
)

/** Le cache-œil du capitaine : le mot de passe est visible, le toucher le cache. */
private val eyePatch = listOf(
    "W.......W",
    ".W.....W.",
    "..WWWWW..",
    "..WPPPW..",
    "..WPPPW..",
    "...WWW...",
    ".........",
)

/** L'icône des champs de mot de passe : œil ouvert ([patched] faux) ou cache-œil. */
@Composable
fun PixelEye(patched: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val sprite = if (patched) eyePatch else openEye
    Canvas(modifier.size(size)) {
        val pixel = this.size.minDimension / sprite.first().length
        // Centré verticalement : la grille est plus large que haute.
        drawSprite(sprite, pixel, Offset(0f, (this.size.height - sprite.size * pixel) / 2))
    }
}
