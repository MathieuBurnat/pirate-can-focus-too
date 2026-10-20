package dev.mathieuburnat.piratefocus.journal

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Le journal de bord rangé dans un fichier sur le téléphone : il survit à la fermeture de l'app. */
class JournalStore(context: Context) : LogBook {

    private val file = journalFile(context)

    override fun load(): List<LogEntry> =
        runCatching { JournalCodec.decode(file.readFully().decodeToString()) }.getOrDefault(emptyList())

    /** Écriture atomique : en cas de coupure en plein milieu, l'ancien fichier reste intact. */
    override fun save(logs: List<LogEntry>) {
        val stream = file.startWrite()
        try {
            stream.write(JournalCodec.encode(logs).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
        }
    }

    companion object {
        /** Le fichier du journal (aussi effacé par le factory reset). */
        fun journalFile(context: Context) = AtomicFile(File(context.applicationContext.filesDir, "journal.tsv"))
    }
}
