package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
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
fun JournalScreen(viewModel: JournalViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val journal = state.journal

    state.adding?.let { side ->
        AddDialog(
            side = side,
            captainLine = state.dialogLine,
            count = journal::count,
            onAdd = viewModel::add,
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
    count: (Entry) -> Int,
    onAdd: (Entry) -> Unit,
    onRemove: (Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    val color = side.color()
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
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface), RectangleShape)
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
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
                    "Appui long pour rayer une ligne.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("TERMINÉ") } },
    )
}
