package dev.swipetune.app

object ReviewQueue {
    fun create(songs: List<SongRef>, decisions: Map<String, ReviewDecision>): List<String> =
        songs.asSequence()
            .map { it.uri }
            .filterNot(decisions::containsKey)
            .toList()
            .shuffled()

    fun applyDecision(
        state: PersistedState,
        decision: ReviewDecision,
    ): PersistedState {
        val uri = state.swipeOrder.getOrNull(state.swipeCursor) ?: return state
        return state.copy(
            decisions = state.decisions + (uri to decision),
            swipeCursor = state.swipeCursor + 1,
            history = state.history + ReviewEntry(uri, decision),
        )
    }

    fun undo(state: PersistedState): PersistedState {
        val last = state.history.lastOrNull() ?: return state
        return state.copy(
            decisions = state.decisions - last.uri,
            swipeCursor = (state.swipeCursor - 1).coerceAtLeast(0),
            history = state.history.dropLast(1),
        )
    }
}

