package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontStyle
import dev.mathieuburnat.piratefocus.journal.Audience
import dev.mathieuburnat.piratefocus.journal.StatsMode
import dev.mathieuburnat.piratefocus.ui.theme.Lagoon
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
    var audience by rememberSaveable { mutableStateOf(Audience.FLOTTE) }
    var shownLogs by remember(period, audience) { mutableIntStateOf(LOGS_PAGE) }
    val now = remember(myLogs) { LocalDateTime.now() }

    val everyone = remember(crew, myLogs, audience) {
        crew.filter { audience == Audience.FLOTTE || it.friend } + CrewMate(ME, myLogs)
    }
    val logs = remember(everyone, period, now) {
        everyone.flatMap { it.logs }.filter { period.contains(it.at, now) }.sortedByDescending { it.at }
    }
    val buckets = remember(logs, period, now) { CrewStats.buckets(logs, period, now) }
    var mode by rememberSaveable { mutableStateOf(StatsMode.ENSEMBLE) }
    val rows = remember(everyone, period, now) { CrewStats.noPainRanking(everyone, period, now) }
    val total = remember(everyone, period, now) { CrewStats.teamTotal(everyone, period, now) }
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
            Tabs(Period.entries, period, { it.label }) { period = it }

            Spacer(Modifier.height(8.dp))
            Tabs(StatsMode.entries, mode, { it.label }) { mode = it }

            Spacer(Modifier.height(16.dp))
            AudiencePicker(audience) { audience = it }

            when (mode) {
                StatsMode.ENSEMBLE -> {
                    TeamTotals(total)
                    Spacer(Modifier.height(16.dp))
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
                }
                StatsMode.NO_PAIN -> {
                    Podium(rows.take(3))
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("-- Classement des biscotos --")
                    Leaderboard(rows)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Les vrais matelots arriveront bientôt.",
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

/** « Toute la flotte ▾ » : un menu déroulant pour ne garder que les amis. */
@Composable
private fun AudiencePicker(audience: Audience, onSelect: (Audience) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    // Centré et encadré, avec un gros triangle : on comprend tout de suite que ça se déroule.
    Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
      // Boîte interne : le menu s'ouvre juste sous le sélecteur, pas contre le bord gauche.
      Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(BorderStroke(2.dp, color), RectangleShape)
                .clickable { open = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(audience.label, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(Modifier.width(12.dp))
            Text(if (open) "▲" else "▼", style = MaterialTheme.typography.titleSmall, color = color)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RectangleShape) {
            Audience.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == audience) "> ${option.label}" else "  ${option.label}") },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
      }
    }
}

@Composable
private fun <T> Tabs(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option == selected
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            Text(
                if (isSelected) "[${label(option)}]" else label(option),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(2.dp, color), RectangleShape)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** ENSEMBLE : les compteurs de tout l'équipage, qui défilent jusqu'au total. */
@Composable
private fun TeamTotals(total: Score) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CountUpBox("BISCOTOS", total.biscotos, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        CountUpBox("TAVERNE", total.taverne, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        CountUpBox("FOCUS", total.traversees, Lagoon, Modifier.weight(1f))
    }
    Text(
        "pour tout l'équipage (FOCUS = traversées terminées)",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@Composable
private fun CountUpBox(title: String, target: Int, color: Color, modifier: Modifier = Modifier) {
    val counter = remember(target) { Animatable(0f) }
    LaunchedEffect(target) { counter.animateTo(target.toFloat(), tween(durationMillis = 1200, easing = LinearOutSlowInEasing)) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .border(BorderStroke(2.dp, color), RectangleShape)
            .padding(vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color)
        Text("%03d".format(counter.value.roundToInt()), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** NO PAIN NO GAIN : le podium des trois plus gros biscotos, marches en blocs qui montent. */
@Composable
private fun Podium(top: List<Pair<String, Score>>) {
    if (top.isEmpty()) return
    val medals = listOf("🥇", "🥈", "🥉")
    // Ordre classique d'un podium : 2e, 1er, 3e.
    val order = listOf(1, 0, 2).filter { it < top.size }
    val heights = mapOf(0 to 96.dp, 1 to 68.dp, 2 to 48.dp)
    val rise = remember(top) { Animatable(0f) }
    LaunchedEffect(top) { rise.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing)) }

    Text(
        "NO PAIN NO GAIN, moussaillons !",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        order.forEach { rank ->
            val (name, score) = top[rank]
            val me = name == ME
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(medals[rank], fontSize = 24.sp)
                Text(
                    name,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
                    color = if (me) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${score.biscotos} 💪", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(heights.getValue(rank) * rise.value)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f + 0.2f * (2 - rank)))
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.secondary), RectangleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${rank + 1}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
    }
}

/**
 * Graphique en barres façon pixel art : pour chaque créneau, une colonne de blocs biscotos (bleus),
 * une de blocs taverne (dorés) et une de blocs focus (verts lagon).
 */
@Composable
private fun PixelBarChart(buckets: List<Bucket>) {
    val sport = MaterialTheme.colorScheme.secondary
    val drink = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val peak = max(1, buckets.maxOfOrNull { maxOf(it.biscotos, it.taverne, it.traversees) } ?: 1)

    // Les blocs montent gentiment, colonne après colonne, à chaque changement de données.
    val grow = remember(buckets) { Animatable(0f) }
    LaunchedEffect(buckets) { grow.animateTo(1f, tween(durationMillis = 1400, easing = LinearOutSlowInEasing)) }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val slot = size.width / buckets.size
        val bar = (slot / 3f) * 0.85f
        // Au-delà de 12 blocs, chaque bloc vaut plusieurs points.
        val maxBlocks = 12
        val perBlock = max(1, ceil(peak / maxBlocks.toFloat()).toInt())
        val block = (size.height - 4f) / maxBlocks
        val gap = block * 0.15f

        fun column(x: Float, value: Int, color: Color, progress: Float) {
            val blocks = ceil(value / perBlock.toFloat()).toInt()
            repeat(ceil(blocks * progress).toInt()) { i ->
                drawRect(
                    color = color,
                    topLeft = Offset(x, size.height - 2f - (i + 1) * block + gap),
                    size = Size(bar, block - gap),
                )
            }
        }

        buckets.forEachIndexed { index, bucket ->
            val x = index * slot + (slot - 3 * bar) / 2f
            // Décalage de départ : la vague de blocs part de la gauche.
            val progress = (grow.value * 1.6f - index / buckets.size.toFloat() * 0.6f).coerceIn(0f, 1f)
            column(x, bucket.biscotos, sport, progress)
            column(x + bar, bucket.taverne, drink, progress)
            column(x + 2 * bar, bucket.traversees, Lagoon, progress)
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
        Text("   ")
        Text("■ FOCUS", color = Lagoon, style = MaterialTheme.typography.labelSmall)
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
    log.note?.let { note ->
        Text(
            "      « $note »",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
        )
    }
}

@Composable
private fun Leaderboard(rows: List<Pair<String, Score>>) {
    Row(Modifier.fillMaxWidth()) {
        Text("#  MATELOT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("BISC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        Text("TAV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        Text("FOC", style = MaterialTheme.typography.labelSmall, color = Lagoon, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
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
            Text("%3d".format(score.traversees), color = Lagoon, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        }
    }
}
