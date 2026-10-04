package dev.mathieuburnat.piratefocus.journal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JournalUiState(
    val journal: JournalState = JournalState(),
    val quote: String = JournalQuotes.verdict(Verdict.PAGE_BLANCHE),
    /** Petite réaction à afficher en toast. */
    val toast: String? = null,
    /** Grande intervention à afficher en pop-up. */
    val popup: String? = null,
)

/** Les compteurs vivent en mémoire : ils repartent à zéro à chaque lancement de l'app. */
class JournalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    fun add(entry: Entry) = _uiState.update { state ->
        val journal = state.journal.add(entry)
        val popup = if (entry.side == Side.BOISSON) JournalQuotes.intervention(journal.total(Side.BOISSON)) else null
        state.copy(
            journal = journal,
            quote = quoteFor(state, journal),
            toast = if (popup == null) JournalQuotes.reaction(entry) else null,
            popup = popup,
        )
    }

    fun remove(entry: Entry) = _uiState.update { state ->
        val journal = state.journal.remove(entry)
        state.copy(journal = journal, quote = quoteFor(state, journal), toast = "Rayé du journal. Personne n'a rien vu.")
    }

    fun newQuote() = _uiState.update {
        it.copy(quote = JournalQuotes.verdict(it.journal.verdict, current = it.quote))
    }

    fun consumeToast() = _uiState.update { it.copy(toast = null) }

    fun dismissPopup() = _uiState.update { it.copy(popup = null) }

    /** Le capitaine change d'avis seulement quand le verdict change. */
    private fun quoteFor(state: JournalUiState, journal: JournalState): String =
        if (journal.verdict != state.journal.verdict) JournalQuotes.verdict(journal.verdict) else state.quote
}
