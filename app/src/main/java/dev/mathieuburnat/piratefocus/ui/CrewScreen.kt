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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.Period
import dev.mathieuburnat.piratefocus.journal.Score
import dev.mathieuburnat.piratefocus.journal.nickname

/** [dev] Les autres matelots, avec des données inventées en attendant le vrai équipage. */
@Composable
fun CrewScreen(crew: List<CrewMate>, myToday: Score, onBack: () -> Unit) {
    var period by rememberSaveable { mutableStateOf(Period.JOUR) }

    // Toi, tu n'as que la journée en cours (le journal n'est pas encore sauvegardé).
    val rows = remember(crew, myToday, period) {
        (crew.map { it.name to it.score(period) } + ("Toi" to myToday))
            .sortedByDescending { (_, score) -> score.biscotos - score.taverne }
    }
    val strongest = rows.maxBy { it.second.biscotos }
    val thirstiest = rows.maxBy { it.second.taverne }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "< JOURNAL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp),
            )
            Text(
                "Et comment se portent les autres matelots ?!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("[dev] équipage imaginaire", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelPirate(phase = Phase.IDLE, size = 72.dp)
                TypewriterText(
                    text = "Le plus costaud : ${strongest.first}. Le plus assoiffé : ${thirstiest.first}. Arr !",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Period.entries.forEach { p ->
                    val selected = p == period
                    Text(
                        if (selected) "[${p.label}]" else p.label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground),
                                RectangleShape,
                            )
                            .clickable { period = p }
                            .padding(vertical = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("#  MATELOT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("BISC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                Text("TAV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
            rows.forEachIndexed { index, (name, score) ->
                val me = name == "Toi"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${index + 1}. $name",
                            fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
                            color = if (me) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("   ${score.verdict.nickname()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("%3d".format(score.biscotos), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                    Text("%3d".format(score.taverne), color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Classement : biscotos moins taverne. Les vrais matelots arriveront bientôt.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
