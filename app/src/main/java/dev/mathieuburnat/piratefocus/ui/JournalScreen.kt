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
                Tally(
                    title = "MUSCLES",
                    side = Side.SPORT,
                    total = journal.total(Side.SPORT),
                    color = MaterialTheme.colorScheme.secondary,
                    count = journal::count,
                    onAdd = viewModel::add,
                    onRemove = viewModel::remove,
                    modifier = Modifier.weight(1f),
                )
                Text("VS", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                Tally(
                    title = "BOUTEILLES",
                    side = Side.BOISSON,
                    total = journal.total(Side.BOISSON),
                    color = MaterialTheme.colorScheme.primary,
                    count = journal::count,
                    onAdd = viewModel::add,
                    onRemove = viewModel::remove,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Appui long pour rayer une ligne.\n📊 Des statistiques étendues seront bientôt disponibles.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tally(
    title: String,
    side: Side,
    total: Int,
    color: Color,
    count: (Entry) -> Int,
    onAdd: (Entry) -> Unit,
    onRemove: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color)
        Text("%02d".format(total), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = color)
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
