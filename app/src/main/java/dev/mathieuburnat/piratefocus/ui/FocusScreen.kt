package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mathieuburnat.piratefocus.focus.FocusState
import dev.mathieuburnat.piratefocus.focus.FocusTimer
import dev.mathieuburnat.piratefocus.focus.FocusUiState
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.focus.PirateQuotes
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme
import kotlinx.coroutines.delay

private val durations = listOf(5, 10, 30)
private const val QUOTE_ROTATION_MS = 20_000L

@Composable
fun FocusRoute(viewModel: FocusViewModel, guardReady: Boolean, onMenu: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FocusScreen(
        state = state,
        guardReady = guardReady,
        onMenu = onMenu,
        onSelectDuration = viewModel::selectDuration,
        onSetSail = viewModel::setSail,
        onAbandon = viewModel::abandonShip,
        onBackToPort = viewModel::backToPort,
        onNewQuote = viewModel::newQuote,
    )
}

@Composable
fun FocusScreen(
    state: FocusUiState,
    guardReady: Boolean,
    onMenu: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSetSail: () -> Unit,
    onAbandon: () -> Unit,
    onBackToPort: () -> Unit,
    onNewQuote: () -> Unit,
) {
    val timer = state.timer
    KeepScreenOn(enabled = timer.phase == Phase.FOCUS)

    // Au port et en escale, le capitaine papote : nouvelle réplique régulièrement.
    val chatty = timer.phase == Phase.IDLE || timer.phase == Phase.BREAK
    LaunchedEffect(chatty) {
        while (chatty) {
            delay(QUOTE_ROTATION_MS)
            onNewQuote()
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable(onClick = onMenu)
                    .padding(vertical = 4.dp),
            )
            Text("=== PIRATE FOCUS ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "doublons: %04d | traversées: %d".format(timer.doubloons, timer.voyages),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))
            PixelPirate(phase = timer.phase)
            QuoteBubble(state.quote, onClick = onNewQuote)

            Spacer(Modifier.weight(1f))
            Text(phaseLabel(timer.phase), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(
                FocusTimer.format(timer.remainingSeconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge,
            )
            Text(asciiProgress(timer.progress), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            PixelShip(phase = timer.phase)
            Waves(animated = timer.phase == Phase.FOCUS)

            Spacer(Modifier.weight(1f))
            if (!guardReady && timer.phase == Phase.IDLE) {
                Text(
                    "! gardien endormi : autorise-le dans Paramètres",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Actions(timer, onSelectDuration, onSetSail, onAbandon, onBackToPort)
        }
    }
}

@Composable
private fun QuoteBubble(quote: String, onClick: () -> Unit) {
    Text(
        text = "« $quote »",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

@Composable
private fun Actions(
    timer: FocusState,
    onSelectDuration: (Int) -> Unit,
    onSetSail: () -> Unit,
    onAbandon: () -> Unit,
    onBackToPort: () -> Unit,
) {
    when (timer.phase) {
        Phase.IDLE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var askingCustom by remember { mutableStateOf(false) }
            val isCustom = timer.focusMinutes !in durations
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                durations.forEach { minutes ->
                    DurationButton(
                        label = "$minutes",
                        selected = minutes == timer.focusMinutes,
                        onClick = { onSelectDuration(minutes) },
                        modifier = Modifier.weight(1f),
                    )
                }
                DurationButton(
                    label = if (isCustom) "${timer.focusMinutes}" else ":",
                    selected = isCustom,
                    onClick = { askingCustom = true },
                    modifier = Modifier.weight(1f),
                )
            }
            if (askingCustom) {
                CustomDurationDialog(
                    initial = timer.focusMinutes,
                    onConfirm = { minutes ->
                        onSelectDuration(minutes)
                        askingCustom = false
                    },
                    onDismiss = { askingCustom = false },
                )
            }
            Spacer(Modifier.height(8.dp))
            PirateButton("> LEVER L'ANCRE", onSetSail)
        }
        Phase.FOCUS -> PirateButton("x ABANDONNER LE NAVIRE", onAbandon, danger = true)
        Phase.BREAK -> PirateButton("> REPRENDRE LA MER", onBackToPort)
        Phase.SUNK -> PirateButton("> RECONSTRUIRE LE NAVIRE", onBackToPort)
    }
}

@Composable
private fun DurationButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RectangleShape,
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground),
        modifier = modifier,
    ) { Text(if (selected) "[$label]" else label, maxLines = 1) }
}

@Composable
private fun CustomDurationDialog(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text("Traversée sur mesure") },
        text = {
            MinutesWheel(
                range = FocusTimer.MIN_MINUTES..FocusTimer.MAX_MINUTES,
                initial = initial,
                onValueChange = { minutes = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }) { Text("CAP !") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULER") }
        },
    )
}

@Composable
private fun PirateButton(label: String, onClick: () -> Unit, danger: Boolean = false) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = if (danger) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun Waves(animated: Boolean) {
    val shift = remember { mutableIntStateOf(0) }
    LaunchedEffect(animated) {
        while (animated) {
            delay(400)
            shift.intValue = (shift.intValue + 1) % 4
        }
    }
    val pattern = "~^~-".repeat(12)
    Text(
        pattern.drop(shift.intValue).take(40),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private fun phaseLabel(phase: Phase) = when (phase) {
    Phase.IDLE -> "AU PORT"
    Phase.FOCUS -> "EN MER"
    Phase.BREAK -> "ESCALE"
    Phase.SUNK -> "NAUFRAGE"
}

private fun asciiProgress(progress: Float, width: Int = 20): String {
    val filled = (progress.coerceIn(0f, 1f) * width).toInt()
    return "[" + "#".repeat(filled) + ".".repeat(width - filled) + "]"
}

@Preview
@Composable
private fun FocusScreenPreview() {
    PirateFocusTheme {
        FocusScreen(
            guardReady = true,
            onMenu = {},
            state = FocusUiState(FocusState(phase = Phase.FOCUS, remainingSeconds = 754, doubloons = 42, voyages = 3), PirateQuotes.randomFor(Phase.FOCUS)),
            onSelectDuration = {},
            onSetSail = {},
            onAbandon = {},
            onBackToPort = {},
            onNewQuote = {},
        )
    }
}
