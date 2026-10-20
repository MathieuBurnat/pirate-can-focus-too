package dev.mathieuburnat.piratefocus.journal

/** Fusion du journal du téléphone avec celui du Worker. Logique pure, testée. */
object JournalSync {

    /**
     * Toutes les lignes connues d'un côté ou de l'autre, sans doublon (même id), de la plus récente à la plus ancienne.
     * Le journal est en ajout seulement : une ligne déjà présente sur le téléphone n'est jamais remplacée.
     */
    fun merge(local: List<LogEntry>, remote: List<LogEntry>): List<LogEntry> {
        val known = local.mapTo(HashSet()) { it.id }
        return (local + remote.filter { it.id !in known }).sortedByDescending { it.at }
    }
}
