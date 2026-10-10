package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Les points d'une face de dé, sur une grille 3 x 3 (ligne, colonne). */
private val pips = mapOf(
    1 to listOf(1 to 1),
    2 to listOf(0 to 0, 2 to 2),
    3 to listOf(0 to 0, 1 to 1, 2 to 2),
    4 to listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2),
    5 to listOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2),
    6 to listOf(0 to 0, 0 to 2, 1 to 0, 1 to 2, 2 to 0, 2 to 2),
)

/** Une face de dé en pixel art (7 x 7) : parchemin et points noirs. */
private fun diceSprite(face: Int): List<String> {
    val grid = List(7) { CharArray(7) { 'W' } }
    pips.getValue(face.coerceIn(1, 6)).forEach { (row, col) -> grid[1 + row * 2][1 + col * 2] = 'K' }
    return grid.map { String(it) }
}

/** Le dé des pseudos de pirate : [face] de 1 à 6. */
@Composable
fun PixelDice(face: Int, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val sprite = diceSprite(face)
    Canvas(modifier.size(size)) {
        drawSprite(sprite, this.size.minDimension / 7, Offset.Zero)
    }
}
