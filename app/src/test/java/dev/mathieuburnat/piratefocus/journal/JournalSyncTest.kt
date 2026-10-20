package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class JournalSyncTest {

    private val now = LocalDateTime.of(2026, 10, 10, 20, 0)

    @Test
    fun `le journal du téléphone et celui du Worker se rejoignent sans doublon`() {
        val shared = LogEntry(ME, Entry.GRIMPE, now.minusDays(1), id = "commun")
        val local = listOf(LogEntry(ME, Entry.BIERE, now, id = "local"), shared)
        val remote = listOf(shared.copy(note = "venu d'ailleurs"), LogEntry(ME, Entry.VIN, now.minusHours(2), id = "distant"))

        val merged = JournalSync.merge(local, remote)
        assertEquals(listOf("local", "distant", "commun"), merged.map { it.id })
        // La ligne du téléphone n'est pas remplacée.
        assertEquals(null, merged.last().note)
    }
}
