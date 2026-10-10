package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.journal.Entry
import dev.mathieuburnat.piratefocus.journal.JournalViewModel
import dev.mathieuburnat.piratefocus.journal.Side
import dev.mathieuburnat.piratefocus.journal.Verdict

private fun Side.title() = if (this == Side.SPORT) "BISCOTOS" else "TAVERNE"

@Composable
private fun Side.color(): Color =
    if (this == Side.SPORT) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

@Composable
fun JournalScreen(viewModel: JournalViewModel, onBack: () -> Unit, onCrew: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val journal = state.journal

    state.adding?.let { side ->
        AddDialog(
            side = side,
            captainLine = state.dialogLine,
            shouting = state.refused,
            note = state.noteDraft,
            onNoteChange = viewModel::editNote,
            count = journal::count,
            onAdd = viewModel::add,
            onInsist = viewModel::insist,
            onRemove = viewModel::remove,
            onDismiss = viewModel::close,
        )
    }

    state.popup?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPopup,
            shape = RectangleShape,
            title = { Text("☠ LE CAPITAINE ☠") },
            text = { TypewriterText(message, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = viewModel::dismissPopup) { Text("OUI CAPITAINE") } },
        )
    }

    state.parrot?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissParrot,
            shape = RectangleShape,
            title = { Text("🦜 COCO LE PERROQUET", color = MaterialTheme.colorScheme.error) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    PixelParrot()
                    TypewriterText(
                        message,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissParrot) { Text("OUI COCO") } },
        )
    }

    val tipsy = journal.verdict == Verdict.EPONGE || journal.verdict == Verdict.PILIER_DE_TAVERNE

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp),
            )
            Text("=== MON JOURNAL ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("🚧 en travaux 🚧", style = MaterialTheme.typography.bodySmall)

            PixelPirate(phase = Phase.IDLE, tipsy = tipsy, size = 150.dp)
            TypewriterText(
                text = "« ${state.quote} »",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
                    .clickable(onClick = viewModel::newQuote)
                    .padding(12.dp),
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ScoreBox(Side.SPORT, journal.total(Side.SPORT), { viewModel.open(Side.SPORT) }, Modifier.weight(1f))
                Text("VS", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                ScoreBox(Side.BOISSON, journal.total(Side.BOISSON), { viewModel.open(Side.BOISSON) }, Modifier.weight(1f))
            }
            Text(
                "Touche un camp pour y ajouter des points.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "> VOIR LES AUTRES MATELOTS [dev]",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
                    .clickable(onClick = onCrew)
                    .padding(14.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "📊 Des statistiques étendues seront bientôt disponibles.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/** La grande case du score d'un camp : la toucher ouvre ses activités. */
@Composable
private fun ScoreBox(side: Side, total: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = side.color()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .border(BorderStroke(2.dp, color), RectangleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(side.title(), style = MaterialTheme.typography.labelLarge, color = color)
        Text("%02d".format(total), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = color)
        Text("+ ajouter", style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Les activités d'un camp, commentées en direct par une petite tête du capitaine. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddDialog(
    side: Side,
    captainLine: String,
    shouting: Boolean,
    note: String,
    onNoteChange: (String) -> Unit,
    count: (Entry) -> Int,
    onAdd: (Entry) -> Unit,
    onInsist: () -> Unit,
    onRemove: (Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    val color = side.color()
    val alarm = MaterialTheme.colorScheme.error

    // Quand le capitaine refuse un verre, sa bulle tremble de colère.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(captainLine, shouting) {
        if (shouting) {
            repeat(4) {
                shake.animateTo(8f, tween(40))
                shake.animateTo(-8f, tween(40))
            }
            shake.animateTo(0f, tween(40))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text("=== ${side.title()} ===", color = color) },
        text = {
            Column {
                // Le capitaine est ravi côté biscotos... et déjà pompette côté taverne.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelPirate(
                        phase = Phase.IDLE,
                        size = 72.dp,
                        happy = side == Side.SPORT,
                        tipsy = side == Side.BOISSON,
                    )
                    TypewriterText(
                        text = captainLine,
                        style = if (shouting) {
                            MaterialTheme.typography.titleMedium.copy(color = alarm, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        charDelayMs = if (shouting) 8 else 18,
                        modifier = Modifier
                            .weight(1f)
                            .offset { IntOffset(shake.value.roundToInt(), 0) }
                            .heightIn(min = 72.dp)
                            .border(
                                BorderStroke(if (shouting) 3.dp else 1.dp, if (shouting) alarm else MaterialTheme.colorScheme.onSurface),
                                RectangleShape,
                            )
                            .padding(8.dp),
                    )
                }
                // Le verre refusé ne compte que si on insiste, exprès, avec ce bouton.
                if (shouting) {
                    Text(
                        "> J'INSISTE 🍺",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = alarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(BorderStroke(2.dp, alarm), RectangleShape)
                            .clickable(onClick = onInsist)
                            .padding(10.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Un petit mot pour le journal de bord, collé au prochain point ajouté.
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    placeholder = {
                        Text(
                            if (side == Side.SPORT) "> un mot sur ta séance ?" else "> un mot sur ta tournée ?",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Entry.entries.filter { it.side == side }.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(BorderStroke(2.dp, color), RectangleShape)
                            .combinedClickable(onClick = { onAdd(entry) }, onLongClick = { onRemove(entry) })
                            .padding(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                            Text(entry.detail, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("+ ${count(entry)}", style = MaterialTheme.typography.titleMedium, color = color)
                    }
                }
                Text(
                    "Le mot (facultatif) part avec le prochain point. Appui long pour rayer une ligne.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("TERMINÉ") } },
    )
}
