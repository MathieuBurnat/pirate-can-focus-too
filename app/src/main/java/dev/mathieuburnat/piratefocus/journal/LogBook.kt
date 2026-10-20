package dev.mathieuburnat.piratefocus.journal

/** Là où le journal de bord est rangé entre deux lancements de l'app. */
interface LogBook {
    fun load(): List<LogEntry>
    fun save(logs: List<LogEntry>)

    /** Un carnet qui ne garde rien : pour les tests et les aperçus. */
    object Forgetful : LogBook {
        override fun load(): List<LogEntry> = emptyList()
        override fun save(logs: List<LogEntry>) = Unit
    }
}
