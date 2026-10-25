package dev.swipetune.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReviewQueueTest {
    private val songs = listOf(
        SongRef("content://song/1", "One.mp3"),
        SongRef("content://song/2", "Two.mp3"),
    )

    @Test
    fun decisionPersistsAndAdvances() {
        val initial = PersistedState(songs = songs, swipeOrder = songs.map(SongRef::uri))
        val next = ReviewQueue.applyDecision(initial, ReviewDecision.REJECTED)

        assertEquals(1, next.swipeCursor)
        assertEquals(ReviewDecision.REJECTED, next.decisions[songs.first().uri])
        assertEquals(1, next.history.size)
    }

    @Test
    fun undoReturnsToPreviousSongAndRemovesDecision() {
        val initial = PersistedState(songs = songs, swipeOrder = songs.map(SongRef::uri))
        val decided = ReviewQueue.applyDecision(initial, ReviewDecision.KEPT)
        val undone = ReviewQueue.undo(decided)

        assertEquals(0, undone.swipeCursor)
        assertFalse(undone.decisions.containsKey(songs.first().uri))
        assertEquals(0, undone.history.size)
    }
}
