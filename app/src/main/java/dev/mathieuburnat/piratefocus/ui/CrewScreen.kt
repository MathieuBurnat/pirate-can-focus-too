package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.journal.Bucket
import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.CrewStats
import dev.mathieuburnat.piratefocus.journal.LogEntry
import dev.mathieuburnat.piratefocus.journal.ME
import dev.mathieuburnat.piratefocus.journal.Period
import dev.mathieuburnat.piratefocus.journal.Score
import dev.mathieuburnat.piratefocus.journal.icon
import dev.mathieuburnat.piratefocus.journal.nickname
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max

private const val LOGS_PAGE = 15

/** [dev] Les autres matelots, avec des données inventées en attendant le vrai équipage. */
@Composable
fun CrewScreen(crew: List<CrewMate>, myLogs: List<LogEntry>, onBack: () -> Unit) {
    var period by rememberSaveable { mutableStateOf(Period.JOUR) }
    var shownLogs by remember(period) { mutableIntStateOf(LOGS_PAGE) }
    val now = remember(myLogs) { LocalDateTime.now() }

    val everyone = remember(crew, myLogs) { crew + CrewMate(ME, myLogs) }
    val logs = remember(everyone, period, now) {
        everyone.flatMap { it.logs }.filter { period.contains(it.at, now) }.sortedByDescending { it.at }
    }
    val buckets = remember(logs, period, now) { CrewStats.buckets(logs, period, now) }
    val rows = remember(everyone, period, now) {
        everyone.map { it.name to it.score(period, now) }
            .sortedByDescending { (_, score) -> score.biscotos - score.taverne }
    }
    // Personne ne gagne un titre avec zéro point.
    val strongest = rows.maxBy { it.second.biscotos }.takeIf { it.second.biscotos > 0 }?.first ?: "personne"
    val thirstiest = rows.maxBy { it.second.taverne }.takeIf { it.second.taverne > 0 }?.first ?: "personne"

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
                    text = "Le plus costaud : $strongest. Le plus assoiffé : $thirstiest. Arr !",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            PeriodTabs(period) { period = it }

            Spacer(Modifier.height(16.dp))
            SectionTitle("-- Toute la flotte --")
            PixelBarChart(buckets)

            Spacer(Modifier.height(16.dp))
            SectionTitle("-- Journal de bord --")
            if (logs.isEmpty()) {
                Text("Rien à signaler, capitaine.", style = MaterialTheme.typography.bodySmall)
            }
            logs.take(shownLogs).forEach { log -> LogLine(log, period) }
            if (logs.size > shownLogs) {
                Text(
                    "> VOIR PLUS (${logs.size - shownLogs})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clickable { shownLogs += LOGS_PAGE }
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("-- Classement --")
            Leaderboard(rows)

            Spacer(Modifier.height(12.dp))
            Text(
                "Classement : biscotos moins taverne. Les vrais matelots arriveront bientôt.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

@Composable
private fun PeriodTabs(period: Period, onSelect: (Period) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Period.entries.forEach { p ->
            val selected = p == period
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            Text(
                if (selected) "[${p.label}]" else p.label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(2.dp, color), RectangleShape)
                    .clickable { onSelect(p) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/**
 * Graphique en barres façon pixel art : pour chaque créneau, une colonne de blocs biscotos (bleus)
 * à gauche et une colonne de blocs taverne (dorés) à droite.
 */
@Composable
private fun PixelBarChart(buckets: List<Bucket>) {
    val sport = MaterialTheme.colorScheme.secondary
    val drink = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val peak = max(1, buckets.maxOfOrNull { max(it.biscotos, it.taverne) } ?: 1)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val slot = size.width / buckets.size
        val bar = (slot / 2f) * 0.8f
        // Au-delà de 12 blocs, chaque bloc vaut plusieurs points.
        val maxBlocks = 12
        val perBlock = max(1, ceil(peak / maxBlocks.toFloat()).toInt())
        val block = (size.height - 4f) / maxBlocks
        val gap = block * 0.15f

        fun column(x: Float, value: Int, color: Color) {
            repeat(ceil(value / perBlock.toFloat()).toInt()) { i ->
                drawRect(
                    color = color,
                    topLeft = Offset(x, size.height - 2f - (i + 1) * block + gap),
                    size = Size(bar, block - gap),
                )
            }
        }

        buckets.forEachIndexed { index, bucket ->
            val x = index * slot + (slot - 2 * bar) / 2f
            column(x, bucket.biscotos, sport)
            column(x + bar, bucket.taverne, drink)
        }
        drawRect(axis, topLeft = Offset(0f, size.height - 2f), size = Size(size.width, 2f))
    }

    Row(Modifier.fillMaxWidth()) {
        Text(buckets.first().label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(buckets.last().label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Text("■ BISCOTOS", color = sport, style = MaterialTheme.typography.labelSmall)
        Text("   ")
        Text("■ TAVERNE", color = drink, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LogLine(log: LogEntry, period: Period) {
    val me = log.who == ME
    val time = if (period == Period.JOUR) {
        "%02d:%02d".format(log.at.hour, log.at.minute)
    } else {
        "%02d/%02d %02d:%02d".format(log.at.dayOfMonth, log.at.monthValue, log.at.hour, log.at.minute)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(
            "  ${log.who}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
            color = if (me) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("${log.entry.icon()} ${log.entry.label}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Leaderboard(rows: List<Pair<String, Score>>) {
    Row(Modifier.fillMaxWidth()) {
        Text("#  MATELOT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("BISC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        Text("TAV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
    }
    rows.forEachIndexed { index, (name, score) ->
        val me = name == ME
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
}
