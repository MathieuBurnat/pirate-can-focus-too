package dev.mathieuburnat.piratefocus.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun JournalScreen(viewModel: JournalViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val journal = state.journal
    val context = LocalContext.current

    // Un seul toast à la fois, même quand on enchaîne les tournées.
    var toast by remember { mutableStateOf<Toast?>(null) }
    LaunchedEffect(state.toast) {
        state.toast?.let { message ->
            toast?.cancel()
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
            viewModel.consumeToast()
        }
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

    var adding by remember { mutableStateOf(false) }
    if (adding) {
        AddDialog(
            count = journal::count,
            onAdd = viewModel::add,
            onRemove = viewModel::remove,
            onDismiss = { adding = false },
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
                ScoreBox(BISCOTOS, journal.total(Side.SPORT), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                Text("VS", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                ScoreBox(TAVERNE, journal.total(Side.BOISSON), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { adding = true }, shape = RectangleShape, modifier = Modifier.fillMaxWidth()) {
                Text("> AJOUTER", style = MaterialTheme.typography.labelLarge)
            }

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

private const val BISCOTOS = "BISCOTOS"
private const val TAVERNE = "TAVERNE"

/** La grande case du score d'un camp. */
@Composable
private fun ScoreBox(title: String, total: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .border(BorderStroke(2.dp, color), RectangleShape)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color)
        Text("%02d".format(total), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = color)
        Text("points", style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Toutes les cases, pour noter ce que tu as fait (ou bu). */
@Composable
private fun AddDialog(
    count: (Entry) -> Int,
    onAdd: (Entry) -> Unit,
    onRemove: (Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text("Qu'as-tu fait, moussaillon ?") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tally(BISCOTOS, Side.SPORT, MaterialTheme.colorScheme.secondary, count, onAdd, onRemove, Modifier.weight(1f))
                    Tally(TAVERNE, Side.BOISSON, MaterialTheme.colorScheme.primary, count, onAdd, onRemove, Modifier.weight(1f))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tally(
    title: String,
    side: Side,
    color: Color,
    count: (Entry) -> Int,
    onAdd: (Entry) -> Unit,
    onRemove: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color)
        Entry.entries.filter { it.side == side }.forEach { entry ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(BorderStroke(2.dp, color), RectangleShape)
                    .combinedClickable(onClick = { onAdd(entry) }, onLongClick = { onRemove(entry) })
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            ) {
                Text(entry.label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text(entry.detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Text("+ ${count(entry)}", style = MaterialTheme.typography.titleMedium, color = color)
            }
        }
    }
}
