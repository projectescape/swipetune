package dev.swipetune.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFinderTest {
    @Test
    fun groupsEquivalentMetadataAndRecommendsLosslessCopy() {
        val mp3 = SongRef("content://song/mp3", "Song.mp3", "Song", "Artist", durationMs = 182_000)
        val flac = SongRef("content://song/flac", "Song.flac", "Song (Remaster)", "Artist", durationMs = 182_700)

        val groups = DuplicateFinder.find(listOf(mp3, flac))

        assertEquals(1, groups.size)
        assertEquals(flac.uri, groups.single().recommendedKeepUri)
    }

    @Test
    fun keepsDifferentDurationsSeparate() {
        val short = SongRef("content://song/short", "Mix.mp3", "Mix", "Artist", durationMs = 120_000)
        val long = SongRef("content://song/long", "Mix.mp3", "Mix", "Artist", durationMs = 180_000)

        assertTrue(DuplicateFinder.find(listOf(short, long)).isEmpty())
    }
}
