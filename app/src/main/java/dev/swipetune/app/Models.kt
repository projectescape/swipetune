package dev.swipetune.app

import android.graphics.Bitmap

data class SongRef(
    val uri: String,
    val fileName: String,
)

data class TrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val artwork: Bitmap? = null,
)

enum class ReviewDecision { KEPT, REJECTED }

data class ReviewEntry(
    val uri: String,
    val decision: ReviewDecision,
)

data class PersistedState(
    val folderUri: String? = null,
    val songs: List<SongRef> = emptyList(),
    val decisions: Map<String, ReviewDecision> = emptyMap(),
    val swipeOrder: List<String> = emptyList(),
    val swipeCursor: Int = 0,
    val history: List<ReviewEntry> = emptyList(),
)

data class AppUiState(
    val folderUri: String? = null,
    val songs: List<SongRef> = emptyList(),
    val decisions: Map<String, ReviewDecision> = emptyMap(),
    val swipeOrder: List<String> = emptyList(),
    val swipeCursor: Int = 0,
    val history: List<ReviewEntry> = emptyList(),
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val nowPlayingUri: String? = null,
    val metadata: TrackMetadata? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
) {
    val visibleSongs: List<SongRef>
        get() = songs.filter { decisions[it.uri] != ReviewDecision.REJECTED }

    val rejectedSongs: List<SongRef>
        get() = songs.filter { decisions[it.uri] == ReviewDecision.REJECTED }

    val currentSwipeSong: SongRef?
        get() = swipeOrder.getOrNull(swipeCursor)?.let { uri -> songs.firstOrNull { it.uri == uri } }

    val reviewedCount: Int get() = decisions.size
}

