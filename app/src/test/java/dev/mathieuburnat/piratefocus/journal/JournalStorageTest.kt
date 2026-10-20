package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class JournalStorageTest {

    /** Un carnet en mémoire, pour voir ce que le ViewModel y range. */
    private class Notebook(var logs: List<LogEntry>) : LogBook {
        override fun load() = logs
        override fun save(logs: List<LogEntry>) {
            this.logs = logs
        }
    }

    @Test
    fun `le journal survit au redémarrage et les compteurs ne gardent que le jour`() {
        val now = LocalDateTime.now()
        val notebook = Notebook(listOf(LogEntry(ME, Entry.BIERE, now.minusDays(2)), LogEntry(ME, Entry.GRIMPE, now)))

        val vm = JournalViewModel(notebook)
        assertEquals(2, vm.uiState.value.myLogs.size)
        assertEquals(1, vm.uiState.value.journal.total(Side.SPORT))
        assertEquals(0, vm.uiState.value.journal.total(Side.BOISSON))

        vm.add(Entry.VIN)
        // La sauvegarde part en arrière-plan : on lui laisse le temps d'accoster.
        val deadline = System.currentTimeMillis() + 2_000
        while (notebook.logs.size < 3 && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertEquals(3, notebook.logs.size)

        val relaunched = JournalViewModel(notebook)
        assertEquals(1, relaunched.uiState.value.journal.count(Entry.VIN))
        assertEquals(Entry.VIN, relaunched.uiState.value.myLogs.first().entry)
    }
}
