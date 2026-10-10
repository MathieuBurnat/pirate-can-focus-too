package dev.mathieuburnat.piratefocus.journal

import java.time.LocalDateTime

/**
 * Le journal de bord mis en texte pour être rangé dans un fichier : une ligne par entrée,
 * champs séparés par des tabulations (id, qui, entrée, date, petit mot). Logique pure, testée.
 */
object JournalCodec {

    private const val SEP = '\t'

    fun encode(logs: List<LogEntry>): String = logs.joinToString(separator = "") { log ->
        listOf(log.id, log.who, log.entry.name, log.at.toString(), log.note.orEmpty())
            .joinToString(SEP.toString(), postfix = "\n") { clean(it) }
    }

    /** Les lignes illisibles (entrée inconnue, date abîmée...) passent par-dessus bord sans faire couler le reste. */
    fun decode(text: String): List<LogEntry> = text.lineSequence().mapNotNull { line ->
        val fields = line.split(SEP)
        if (fields.size < 4) return@mapNotNull null
        val entry = Entry.entries.firstOrNull { it.name == fields[2] } ?: return@mapNotNull null
        val at = runCatching { LocalDateTime.parse(fields[3]) }.getOrNull() ?: return@mapNotNull null
        LogEntry(
            who = fields[1],
            entry = entry,
            at = at,
            note = fields.getOrNull(4)?.ifEmpty { null },
            id = fields[0],
        )
    }.toList()

    /** Une tabulation ou un retour à la ligne dans un petit mot casserait le fichier : on les remplace par des espaces. */
    private fun clean(field: String): String = field.replace(Regex("[\\t\\r\\n]"), " ")
}
