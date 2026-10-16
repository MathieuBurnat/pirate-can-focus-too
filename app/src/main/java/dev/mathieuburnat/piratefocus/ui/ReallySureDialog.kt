package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Secondes à attendre avant de pouvoir confirmer. */
private const val CONFIRM_WAIT_S = 5

/**
 * Dernière chance avant une action sans retour (factory reset, liste noire vidée...) :
 * « CONFIRMER » ne répond qu'après [CONFIRM_WAIT_S] secondes.
 */
@Composable
fun ReallySureDialog(message: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    var left by remember { mutableIntStateOf(CONFIRM_WAIT_S) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RectangleShape,
        title = { Text("☠ ATTENTION ☠", maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
        text = {
            TypewriterText(
                message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { ConfirmButton(left, onConfirm) },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    "non, annuler capitaine !",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
    )
}

/**
 * « CONFIRMER » en rouge : bordure blanche en pointillés et compte à rebours tant qu'il faut patienter,
 * puis, d'un coup, bordure rouge pleine et bouton cliquable.
 */
@Composable
private fun ConfirmButton(left: Int, onConfirm: () -> Unit) {
    val ready = left == 0
    val red = MaterialTheme.colorScheme.error
    Text(
        if (ready) "CONFIRMER" else "CONFIRMER ($left)",
        style = MaterialTheme.typography.labelLarge,
        color = if (ready) red else red.copy(alpha = 0.5f),
        modifier = Modifier
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = if (ready) red else Color.White,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (ready) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(enabled = ready, onClick = onConfirm)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
