package dev.swipetune.app

import android.graphics.Bitmap

data class SongRef(
    val uri: String,
    val fileName: String,
    val title: String = fileName.substringBeforeLast('.'),
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val folderName: String = "Music",
    val durationMs: Long = 0,
    val trackNumber: Int = 0,
)

data class TrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val artwork: Bitmap? = null,
)

enum class ReviewDecision { KEPT, REJECTED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class RepeatMode { OFF, ALL, ONE }

data class ReviewEntry(
    val uri: String,
    val decision: ReviewDecision,
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val songUris: List<String> = emptyList(),
)

data class PersistedState(
    val folderUri: String? = null,
    val songs: List<SongRef> = emptyList(),
    val decisions: Map<String, ReviewDecision> = emptyMap(),
    val swipeOrder: List<String> = emptyList(),
    val swipeCursor: Int = 0,
    val history: List<ReviewEntry> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val playlists: List<UserPlaylist> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
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
    val artworkByUri: Map<String, Bitmap> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val playlists: List<UserPlaylist> = emptyList(),
    val playbackQueue: List<String> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
) {
    val visibleSongs: List<SongRef>
        get() = songs.filter { decisions[it.uri] != ReviewDecision.REJECTED }

    val rejectedSongs: List<SongRef>
        get() = songs.filter { decisions[it.uri] == ReviewDecision.REJECTED }

    val currentSwipeSong: SongRef?
        get() = swipeOrder.getOrNull(swipeCursor)?.let { uri -> songs.firstOrNull { it.uri == uri } }

    val reviewedCount: Int get() = decisions.size

    val nowPlayingSong: SongRef?
        get() = songs.firstOrNull { it.uri == nowPlayingUri }

    fun isFavorite(song: SongRef): Boolean = song.uri in favorites
}

