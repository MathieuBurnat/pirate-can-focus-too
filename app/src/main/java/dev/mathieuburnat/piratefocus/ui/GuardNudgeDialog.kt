package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.mathieuburnat.piratefocus.focus.Phase

/** Le capitaine, tout sourire, réclame ses autorisations pour pouvoir aborder les sirènes. */
@Composable
fun GuardNudgeDialog(text: String, onFollow: () -> Unit, onLater: () -> Unit) {
    Dialog(onDismissRequest = onLater) {
        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                PixelPirate(phase = Phase.IDLE, happy = true, lookingAtYou = true, size = 112.dp)
                Spacer(Modifier.height(12.dp))
                TypewriterText(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                MenuItem("SUIVRE LE CAPITAINE", onClick = onFollow)
                TextButton(onClick = onLater) {
                    Text("plus tard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
