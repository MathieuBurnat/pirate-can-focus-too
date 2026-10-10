package dev.mathieuburnat.piratefocus.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class JournalCodecTest {

    private val now = LocalDateTime.of(2026, 10, 10, 21, 30, 15)

    @Test
    fun `le journal ressort du fichier tel qu'il y est entré`() {
        val logs = listOf(
            LogEntry(ME, Entry.BIERE, now, "à la santé du capitaine"),
            LogEntry(ME, Entry.GRIMPE, now.minusDays(1), null),
            LogEntry(ME, Entry.TRAVERSEE, now.minusHours(3), "30 min"),
        )
        assertEquals(logs, JournalCodec.decode(JournalCodec.encode(logs)))
    }

    @Test
    fun `les tabulations et retours à la ligne d'un petit mot ne cassent pas le fichier`() {
        val log = LogEntry(ME, Entry.VIN, now, "rouge\tpuis\nblanc")
        val back = JournalCodec.decode(JournalCodec.encode(listOf(log))).single()
        assertEquals("rouge puis blanc", back.note)
        assertEquals(log.id, back.id)
    }

    @Test
    fun `les lignes abîmées passent par-dessus bord`() {
        val good = LogEntry(ME, Entry.ABDOS, now)
        val text = "n'importe quoi\n" +
            "id1\tToi\tKRAKEN\t2026-10-10T10:00\t\n" +
            "id2\tToi\tBIERE\tpas une date\t\n" +
            JournalCodec.encode(listOf(good))
        val back = JournalCodec.decode(text).single()
        assertEquals(good, back)
        assertNull(back.note)
    }

    @Test
    fun `un fichier vide donne un journal vide`() {
        assertEquals(emptyList<LogEntry>(), JournalCodec.decode(""))
    }

    @Test
    fun `les compteurs du jour ignorent les jours précédents`() {
        val logs = listOf(
            LogEntry(ME, Entry.BIERE, now),
            LogEntry(ME, Entry.BIERE, now.withHour(1)),
            LogEntry(ME, Entry.VIN, now.minusDays(1)),
            LogEntry(ME, Entry.GRIMPE, now.minusDays(3)),
        )
        val today = JournalState.today(logs, LocalDate.of(2026, 10, 10))
        assertEquals(2, today.count(Entry.BIERE))
        assertEquals(0, today.count(Entry.VIN))
        assertEquals(0, today.total(Side.SPORT))
    }
}
