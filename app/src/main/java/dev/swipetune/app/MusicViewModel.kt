package dev.swipetune.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SessionStore(application)
    private val repository = MusicRepository(application)
    private val player = ExoPlayer.Builder(application).build()
    private val _state = MutableStateFlow(store.load().toUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) = updatePlaybackPosition()
        })
        viewModelScope.launch {
            while (isActive) {
                updatePlaybackPosition()
                delay(400)
            }
        }
    }

    fun loadFolder(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, errorMessage = null, statusMessage = "Scanning folder…") }
            runCatching {
                val songs = withContext(Dispatchers.IO) { repository.scan(uri) }
                val old = _state.value
                val validUris = songs.mapTo(hashSetOf(), SongRef::uri)
                val decisions = old.decisions.filterKeys(validUris::contains)
                val order = ReviewQueue.create(songs, decisions)
                PersistedState(
                    folderUri = uri.toString(),
                    songs = songs,
                    decisions = decisions,
                    swipeOrder = order,
                )
            }.onSuccess { persisted ->
                saveAndShow(persisted)
                _state.update {
                    it.copy(isScanning = false, statusMessage = "Found ${persisted.songs.size} songs")
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isScanning = false, statusMessage = null, errorMessage = error.message ?: "Could not scan folder")
                }
            }
        }
    }

    fun play(song: SongRef, autoplay: Boolean = true) {
        _state.update { it.copy(nowPlayingUri = song.uri, metadata = null, errorMessage = null) }
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.playWhenReady = autoplay
        viewModelScope.launch {
            val metadata = runCatching { withContext(Dispatchers.IO) { repository.metadata(song) } }
                .getOrElse { TrackMetadata(song.fileName.substringBeforeLast('.'), "Unknown artist", "Unknown album") }
            if (_state.value.nowPlayingUri == song.uri) _state.update { it.copy(metadata = metadata) }
        }
    }

    fun playCurrentSwipe() {
        _state.value.currentSwipeSong?.let(::play)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        updatePlaybackPosition()
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun nextVisibleSong() {
        val songs = _state.value.visibleSongs
        val index = songs.indexOfFirst { it.uri == _state.value.nowPlayingUri }
        songs.getOrNull(index + 1)?.let(::play)
    }

    fun previousVisibleSong() {
        val songs = _state.value.visibleSongs
        val index = songs.indexOfFirst { it.uri == _state.value.nowPlayingUri }
        songs.getOrNull(index - 1)?.let(::play)
    }

    fun shuffleLibrary() {
        _state.value.visibleSongs.randomOrNull()?.let(::play)
    }

    fun decide(decision: ReviewDecision) {
        val current = _state.value.toPersisted()
        if (current.swipeOrder.getOrNull(current.swipeCursor) == null) return
        val next = ReviewQueue.applyDecision(current, decision)
        saveAndShow(next)
        _state.value.currentSwipeSong?.let(::play) ?: run {
            player.pause()
            if (decision == ReviewDecision.REJECTED) clearNowPlaying()
        }
    }

    fun undo() {
        val current = _state.value.toPersisted()
        if (current.history.isEmpty()) return
        val next = ReviewQueue.undo(current)
        saveAndShow(next)
        _state.value.currentSwipeSong?.let(::play)
    }

    fun restore(song: SongRef) {
        val current = _state.value.toPersisted()
        val nextDecisions = current.decisions - song.uri
        val remainingOrder = current.swipeOrder.drop(current.swipeCursor).filterNot { it == song.uri }
        val reviewedOrder = current.swipeOrder.take(current.swipeCursor)
        saveAndShow(current.copy(
            decisions = nextDecisions,
            swipeOrder = reviewedOrder + song.uri + remainingOrder,
        ))
    }

    fun restartReview() {
        val current = _state.value.toPersisted()
        val order = ReviewQueue.create(current.songs, current.decisions)
        saveAndShow(current.copy(swipeOrder = order, swipeCursor = 0, history = emptyList()))
    }

    fun deleteRejected() {
        viewModelScope.launch {
            val rejected = _state.value.rejectedSongs
            if (rejected.isEmpty()) return@launch
            _state.update { it.copy(isDeleting = true, statusMessage = "Deleting rejected files…") }
            val deleted = withContext(Dispatchers.IO) { rejected.filter(repository::delete) }
            val deletedUris = deleted.mapTo(hashSetOf(), SongRef::uri)
            val deletedCurrentTrack = _state.value.nowPlayingUri in deletedUris
            val current = _state.value.toPersisted()
            val next = current.copy(
                songs = current.songs.filterNot { it.uri in deletedUris },
                decisions = current.decisions.filterKeys { it !in deletedUris },
                swipeOrder = current.swipeOrder.filterNot { it in deletedUris },
                history = current.history.filterNot { it.uri in deletedUris },
            )
            saveAndShow(next)
            if (deletedCurrentTrack) {
                player.stop()
                clearNowPlaying()
            }
            _state.update {
                it.copy(
                    isDeleting = false,
                    statusMessage = "Deleted ${deleted.size} of ${rejected.size} rejected files",
                )
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(statusMessage = null, errorMessage = null) }

    private fun saveAndShow(persisted: PersistedState) {
        store.save(persisted)
        val playback = _state.value
        _state.value = persisted.toUiState().copy(
            nowPlayingUri = playback.nowPlayingUri,
            metadata = playback.metadata,
            isPlaying = playback.isPlaying,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            isScanning = playback.isScanning,
            isDeleting = playback.isDeleting,
            statusMessage = playback.statusMessage,
            errorMessage = playback.errorMessage,
        )
    }

    private fun updatePlaybackPosition() {
        _state.update {
            it.copy(
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.coerceAtLeast(0),
            )
        }
    }

    private fun clearNowPlaying() {
        _state.update {
            it.copy(
                nowPlayingUri = null,
                metadata = null,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
            )
        }
    }

    override fun onCleared() {
        player.release()
    }
}

private fun PersistedState.toUiState() = AppUiState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor.coerceIn(0, swipeOrder.size),
    history = history,
)

private fun AppUiState.toPersisted() = PersistedState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor,
    history = history,
)

