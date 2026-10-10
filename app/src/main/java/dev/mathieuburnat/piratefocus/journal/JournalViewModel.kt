package dev.mathieuburnat.piratefocus.journal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/** Ton nom dans le journal de bord et le classement. */
const val ME = "Toi"

data class JournalUiState(
    val journal: JournalState = JournalState(),
    /** Tes lignes du journal de bord, de la plus récente à la plus ancienne. */
    val myLogs: List<LogEntry> = emptyList(),
    /** Le petit mot (facultatif) qui accompagnera le prochain point ajouté. */
    val noteDraft: String = "",
    val quote: String = JournalQuotes.verdict(Verdict.PAGE_BLANCHE),
    /** Le camp dont la fenêtre d'ajout est ouverte (null = fermée). */
    val adding: Side? = null,
    /** Ce que dit la petite tête du capitaine dans la fenêtre d'ajout. */
    val dialogLine: String = "",
    /** Le verre que le capitaine vient de refuser : il ne compte que si on appuie sur « J'INSISTE ». */
    val pending: Entry? = null,
    /** Grande intervention à afficher en pop-up. */
    val popup: String? = null,
    /** Coco le perroquet débarque (trop de verres dans la soirée). */
    val parrot: String? = null,
) {
    /** Le capitaine est en train de crier « NON ! PAS ENCORE ! ». */
    val refused: Boolean get() = pending != null
}

/** Les compteurs vivent en mémoire : ils repartent à zéro à chaque lancement de l'app. */
class JournalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    /** [dev] Un équipage imaginaire, tiré au sort à chaque lancement. */
    val crew: List<CrewMate> = FakeCrew.generate(LocalDateTime.now())

    fun open(side: Side) = _uiState.update {
        it.copy(adding = side, dialogLine = JournalQuotes.greeting(side), pending = null)
    }

    fun close() = _uiState.update { it.copy(adding = null, noteDraft = "", pending = null) }

    fun editNote(note: String) = _uiState.update { it.copy(noteDraft = note.take(80)) }

    fun add(entry: Entry) = _uiState.update { state ->
        // Passé le quota, le capitaine refuse le verre : rien n'est compté, il faut insister exprès.
        if (JournalRules.refuses(entry, state.journal, insisting = false)) {
            state.copy(dialogLine = JournalQuotes.refusal(current = state.dialogLine), pending = entry)
        } else {
            commit(state, entry, insisted = false)
        }
    }

    /** « J'INSISTE » : le capitaine cède, et le verre refusé est enfin noté. */
    fun insist() = _uiState.update { state ->
        state.pending?.let { commit(state, it, insisted = true) } ?: state
    }

    /** Note le point pour de bon : compteurs, journal de bord, réactions et pop-ups. */
    private fun commit(state: JournalUiState, entry: Entry, insisted: Boolean): JournalUiState {
        val journal = state.journal.add(entry)
        val drinks = journal.total(Side.BOISSON)
        val isDrink = entry.side == Side.BOISSON
        // Coco le perroquet a la priorité sur le capitaine.
        val parrot = if (isDrink && JournalRules.parrotAppears(drinks)) JournalQuotes.parrot(drinks) else null
        val popup = if (isDrink && parrot == null) JournalQuotes.intervention(drinks) else null
        return state.copy(
            journal = journal,
            myLogs = listOf(LogEntry(ME, entry, LocalDateTime.now(), state.noteDraft.trim().ifEmpty { null })) + state.myLogs,
            noteDraft = "",
            quote = quoteFor(state, journal),
            dialogLine = if (insisted) JournalQuotes.giveIn() else JournalQuotes.reaction(entry),
            pending = null,
            popup = popup,
            parrot = parrot,
        )
    }

    fun remove(entry: Entry) = _uiState.update { state ->
        val journal = state.journal.remove(entry)
        // On raye la dernière ligne de ce type (les logs sont rangés du plus récent au plus ancien).
        val index = state.myLogs.indexOfFirst { it.entry == entry }
        val myLogs = if (index >= 0) state.myLogs.filterIndexed { i, _ -> i != index } else state.myLogs
        state.copy(journal = journal, myLogs = myLogs, quote = quoteFor(state, journal), dialogLine = JournalQuotes.ERASED, pending = null)
    }

    fun newQuote() = _uiState.update {
        it.copy(quote = JournalQuotes.verdict(it.journal.verdict, current = it.quote))
    }

    fun dismissPopup() = _uiState.update { it.copy(popup = null) }

    fun dismissParrot() = _uiState.update { it.copy(parrot = null) }

    /** Le capitaine change d'avis seulement quand le verdict change. */
    private fun quoteFor(state: JournalUiState, journal: JournalState): String =
        if (journal.verdict != state.journal.verdict) JournalQuotes.verdict(journal.verdict) else state.quote
}
