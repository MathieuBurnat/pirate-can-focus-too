package dev.mathieuburnat.piratefocus.focus

enum class Phase {
    /** Au port, prêt à lever l'ancre. */
    IDLE,

    /** En pleine traversée : on se concentre. */
    FOCUS,

    /** Escale au port : pause méritée. */
    BREAK,

    /** Le navire a coulé (session abandonnée). */
    SUNK,
}

data class FocusState(
    val phase: Phase = Phase.IDLE,
    val focusMinutes: Int = 30,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 30 * 60,
    val totalSeconds: Int = 30 * 60,
    val doubloons: Int = 0,
    val voyages: Int = 0,
) {
    /** Avancement de la phase en cours, de 0f à 1f. */
    val progress: Float
        get() = if (totalSeconds == 0) 0f else 1f - remainingSeconds.toFloat() / totalSeconds
}

/** Logique pure du minuteur, sans dépendance Android. */
object FocusTimer {

    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 180

    fun selectDuration(state: FocusState, minutes: Int): FocusState {
        if (state.phase != Phase.IDLE) return state
        val safeMinutes = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        return state.copy(focusMinutes = safeMinutes, remainingSeconds = safeMinutes * 60, totalSeconds = safeMinutes * 60)
    }

    fun setSail(state: FocusState): FocusState {
        val seconds = state.focusMinutes * 60
        return state.copy(phase = Phase.FOCUS, remainingSeconds = seconds, totalSeconds = seconds)
    }

    fun tick(state: FocusState): FocusState {
        if (state.phase != Phase.FOCUS && state.phase != Phase.BREAK) return state
        if (state.remainingSeconds > 1) return state.copy(remainingSeconds = state.remainingSeconds - 1)

        return when (state.phase) {
            Phase.FOCUS -> {
                val breakSeconds = state.breakMinutes * 60
                state.copy(
                    phase = Phase.BREAK,
                    remainingSeconds = breakSeconds,
                    totalSeconds = breakSeconds,
                    doubloons = state.doubloons + rewardFor(state.focusMinutes),
                    voyages = state.voyages + 1,
                )
            }
            else -> backToPort(state)
        }
    }

    fun abandonShip(state: FocusState): FocusState =
        if (state.phase == Phase.FOCUS) state.copy(phase = Phase.SUNK, remainingSeconds = 0) else state

    fun backToPort(state: FocusState): FocusState {
        val seconds = state.focusMinutes * 60
        return state.copy(phase = Phase.IDLE, remainingSeconds = seconds, totalSeconds = seconds)
    }

    /** Un doublon par minute de concentration, plus un bonus pour les longues traversées. */
    fun rewardFor(minutes: Int): Int = minutes + if (minutes >= 50) 10 else 0

    fun format(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
}
