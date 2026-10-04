package dev.mathieuburnat.piratefocus.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FocusUiState(
    val timer: FocusState = FocusState(),
    val quote: String = PirateQuotes.randomFor(Phase.IDLE),
)

class FocusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null

    fun selectDuration(minutes: Int) = transition { FocusTimer.selectDuration(it, minutes) }

    fun setSail() {
        transition(FocusTimer::setSail)
        startTicking()
    }

    fun abandonShip() {
        ticker?.cancel()
        transition(FocusTimer::abandonShip)
    }

    fun backToPort() {
        ticker?.cancel()
        transition(FocusTimer::backToPort)
    }

    /** Le capitaine change de réplique (au port, en escale, ou quand on tape sur la bulle). */
    fun newQuote() {
        _uiState.update { it.copy(quote = PirateQuotes.randomFor(it.timer.phase, current = it.quote)) }
    }

    private fun startTicking() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                transition(FocusTimer::tick)
                val phase = _uiState.value.timer.phase
                if (phase != Phase.FOCUS && phase != Phase.BREAK) break
            }
        }
    }

    /** Applique une transition et change de réplique quand la phase change. */
    private fun transition(change: (FocusState) -> FocusState) {
        _uiState.update { current ->
            val next = change(current.timer)
            val quote = if (next.phase != current.timer.phase) PirateQuotes.randomFor(next.phase) else current.quote
            FocusUiState(timer = next, quote = quote)
        }
    }
}
