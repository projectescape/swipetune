package dev.swipetune.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SessionStore(application)
    private val repository = MusicRepository(application)
    private val _state = MutableStateFlow(store.load().toUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    private val controllerFuture: ListenableFuture<MediaController>
    private var player: MediaController? = null
    private var playbackQueue: List<SongRef> = emptyList()
    private val loadingArtwork = mutableSetOf<String>()
    private var reviewModeActive = false
    private var normalPlayback: NormalPlaybackSnapshot? = null

    private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackPosition()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val uri = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
                val song = _state.value.songs.firstOrNull { it.uri == uri } ?: return
                showCurrentSong(song)
                syncQueueFromPlayer()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.update { it.copy(repeatMode = repeatMode.toAppRepeatMode()) }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        errorMessage = "Could not play this file. It may have moved or become unavailable.",
                    )
                }
            }
        }

    init {
        val token = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, token).buildAsync()
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess(::attachPlayer)
                    .onFailure { error ->
                        _state.update {
                            it.copy(errorMessage = error.message ?: "Could not connect to the music player")
                        }
                    }
            },
            ContextCompat.getMainExecutor(application),
        )
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
                    favorites = old.favorites.intersect(validUris),
                    playlists = old.playlists.map { playlist ->
                        playlist.copy(songUris = playlist.songUris.filter(validUris::contains))
                    },
                    themeMode = old.themeMode,
                    dynamicColor = old.dynamicColor,
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
        playQueue(_state.value.visibleSongs, song, autoplay)
    }

    fun playReviewSong(song: SongRef) = playQueue(listOf(song), song, autoplay = true)

    fun playCollection(songs: List<SongRef>, start: SongRef? = null) {
        val first = start ?: songs.firstOrNull() ?: return
        playQueue(songs, first, autoplay = true)
    }

    fun playCurrentSwipe() {
        _state.value.currentSwipeSong?.let(::playReviewSong)
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                if (it.playbackState == Player.STATE_ENDED) it.seekTo(0)
                it.play()
            }
        }
    }

    fun enterReviewMode() {
        if (reviewModeActive) return
        reviewModeActive = true
        val player = player ?: return
        normalPlayback = NormalPlaybackSnapshot(
            items = List(player.mediaItemCount, player::getMediaItemAt),
            index = player.currentMediaItemIndex.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0),
            playWhenReady = player.playWhenReady,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    fun exitReviewMode() {
        if (!reviewModeActive) return
        reviewModeActive = false
        val player = player ?: return
        val snapshot = normalPlayback
        normalPlayback = null
        if (snapshot == null || snapshot.items.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            clearNowPlaying()
            return
        }
        player.setMediaItems(snapshot.items, snapshot.index.coerceIn(snapshot.items.indices), snapshot.positionMs)
        player.shuffleModeEnabled = snapshot.shuffle
        player.repeatMode = snapshot.repeatMode
        player.prepare()
        player.playWhenReady = snapshot.playWhenReady
        syncQueueFromPlayer()
        snapshot.items.getOrNull(snapshot.index)?.mediaId?.let { uri ->
            _state.value.songs.firstOrNull { it.uri == uri }?.let(::showCurrentSong)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = player ?: return
        player.seekTo(positionMs.coerceIn(0, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        updatePlaybackPosition()
    }

    fun seekBy(deltaMs: Long) {
        val player = player ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun nextVisibleSong() {
        player?.takeIf(Player::hasNextMediaItem)?.seekToNextMediaItem()
    }

    fun previousVisibleSong() {
        val player = player ?: return
        if (player.currentPosition > 3_000) player.seekTo(0)
        else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    fun shuffleLibrary() {
        val songs = _state.value.visibleSongs.shuffled()
        songs.firstOrNull()?.let { playQueue(songs, it, autoplay = true) }
    }

    fun toggleShuffle() {
        player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val player = player ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playQueueItem(uri: String) {
        val player = player ?: return
        val index = (0 until player.mediaItemCount).firstOrNull {
            player.getMediaItemAt(it).mediaId == uri
        } ?: return
        player.seekToDefaultPosition(index)
        player.play()
    }

    fun removeQueueItem(uri: String) {
        val player = player ?: return
        val index = (0 until player.mediaItemCount).firstOrNull {
            player.getMediaItemAt(it).mediaId == uri
        } ?: return
        if (player.mediaItemCount == 1) {
            player.stop()
            player.clearMediaItems()
            clearNowPlaying()
        } else {
            player.removeMediaItem(index)
            syncQueueFromPlayer()
        }
    }

    fun toggleFavorite(song: SongRef) {
        val current = _state.value.toPersisted()
        val favorites = if (song.uri in current.favorites) current.favorites - song.uri else current.favorites + song.uri
        saveAndShow(current.copy(favorites = favorites))
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(
                playlists = current.playlists + UserPlaylist(UUID.randomUUID().toString(), cleanName),
            ),
        )
    }

    fun deletePlaylist(id: String) {
        val current = _state.value.toPersisted()
        saveAndShow(current.copy(playlists = current.playlists.filterNot { it.id == id }))
    }

    fun addToPlaylist(playlistId: String, song: SongRef) {
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(
                playlists = current.playlists.map { playlist ->
                    if (playlist.id == playlistId && song.uri !in playlist.songUris) {
                        playlist.copy(songUris = playlist.songUris + song.uri)
                    } else playlist
                },
            ),
        )
    }

    fun removeFromPlaylist(playlistId: String, song: SongRef) {
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(
                playlists = current.playlists.map { playlist ->
                    if (playlist.id == playlistId) playlist.copy(songUris = playlist.songUris - song.uri) else playlist
                },
            ),
        )
    }

    fun requestArtwork(song: SongRef) {
        if (song.uri in _state.value.artworkByUri || !loadingArtwork.add(song.uri)) return
        viewModelScope.launch {
            val art = runCatching { withContext(Dispatchers.IO) { repository.artwork(song) } }.getOrNull()
            loadingArtwork -= song.uri
            art?.let { bitmap -> _state.update { it.copy(artworkByUri = it.artworkByUri + (song.uri to bitmap)) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        saveAndShow(_state.value.toPersisted().copy(themeMode = mode))
    }

    fun setDynamicColor(enabled: Boolean) {
        saveAndShow(_state.value.toPersisted().copy(dynamicColor = enabled))
    }

    fun rescanLibrary() {
        _state.value.folderUri?.let { loadFolder(Uri.parse(it)) }
    }

    fun decide(decision: ReviewDecision) {
        val current = _state.value.toPersisted()
        if (current.swipeOrder.getOrNull(current.swipeCursor) == null) return
        val next = ReviewQueue.applyDecision(current, decision)
        saveAndShow(next)
        _state.value.currentSwipeSong?.let(::playReviewSong) ?: run {
            player?.pause()
            player?.clearMediaItems()
            if (decision == ReviewDecision.REJECTED) clearNowPlaying()
        }
    }

    fun undo() {
        val current = _state.value.toPersisted()
        if (current.history.isEmpty()) return
        val next = ReviewQueue.undo(current)
        saveAndShow(next)
        _state.value.currentSwipeSong?.let(::playReviewSong)
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
                playlists = current.playlists.map { playlist ->
                    playlist.copy(songUris = playlist.songUris.filterNot { it in deletedUris })
                },
            )
            saveAndShow(next)
            if (deletedCurrentTrack) {
                player?.stop()
                player?.clearMediaItems()
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
            artworkByUri = playback.artworkByUri,
            playbackQueue = playback.playbackQueue,
            shuffleEnabled = playback.shuffleEnabled,
            repeatMode = playback.repeatMode,
        )
    }

    private fun playQueue(songs: List<SongRef>, start: SongRef, autoplay: Boolean) {
        if (songs.isEmpty()) return
        val player = player ?: run {
            _state.update { it.copy(errorMessage = "The player is still starting. Try again in a moment.") }
            return
        }
        playbackQueue = songs
        val startIndex = songs.indexOfFirst { it.uri == start.uri }.coerceAtLeast(0)
        _state.update {
            it.copy(
                nowPlayingUri = start.uri,
                metadata = TrackMetadata(start.title, start.artist, start.album, it.artworkByUri[start.uri]),
                playbackQueue = songs.map(SongRef::uri),
                errorMessage = null,
            )
        }
        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setTrackNumber(song.trackNumber.takeIf { it > 0 })
                        .build(),
                )
                .build()
        }
        player.setMediaItems(items, startIndex, 0)
        player.prepare()
        player.playWhenReady = autoplay
        showCurrentSong(start)
    }

    private fun showCurrentSong(song: SongRef) {
        _state.update {
            it.copy(
                nowPlayingUri = song.uri,
                metadata = TrackMetadata(song.title, song.artist, song.album, it.artworkByUri[song.uri]),
            )
        }
        viewModelScope.launch {
            val metadata = runCatching { withContext(Dispatchers.IO) { repository.metadata(song) } }
                .getOrElse { TrackMetadata(song.title, song.artist, song.album) }
            if (_state.value.nowPlayingUri == song.uri) {
                _state.update { current ->
                    current.copy(
                        metadata = metadata,
                        artworkByUri = metadata.artwork?.let { current.artworkByUri + (song.uri to it) }
                            ?: current.artworkByUri,
                    )
                }
            }
        }
    }

    private fun updatePlaybackPosition() {
        val player = player ?: return
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
                playbackQueue = emptyList(),
            )
        }
    }

    override fun onCleared() {
        player?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        player = null
    }

    private fun attachPlayer(controller: MediaController) {
        player = controller
        controller.addListener(playerListener)
        syncQueueFromPlayer()
        val currentUri = controller.currentMediaItem?.mediaId?.takeIf(String::isNotBlank)
        currentUri?.let { uri -> _state.value.songs.firstOrNull { it.uri == uri }?.let(::showCurrentSong) }
        _state.update {
            it.copy(
                isPlaying = controller.isPlaying,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode.toAppRepeatMode(),
                errorMessage = null,
            )
        }
        updatePlaybackPosition()
    }

    private fun syncQueueFromPlayer() {
        val player = player ?: return
        val queueUris = buildList {
            repeat(player.mediaItemCount) { index ->
                player.getMediaItemAt(index).mediaId.takeIf(String::isNotBlank)?.let(::add)
            }
        }
        playbackQueue = queueUris.mapNotNull { uri -> _state.value.songs.firstOrNull { it.uri == uri } }
        _state.update { it.copy(playbackQueue = queueUris) }
    }
}

private fun PersistedState.toUiState() = AppUiState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor.coerceIn(0, swipeOrder.size),
    history = history,
    favorites = favorites,
    playlists = playlists,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
)

private fun AppUiState.toPersisted() = PersistedState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor,
    history = history,
    favorites = favorites,
    playlists = playlists,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
)

private fun Int.toAppRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    else -> RepeatMode.OFF
}

private data class NormalPlaybackSnapshot(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
    val playWhenReady: Boolean,
    val shuffle: Boolean,
    val repeatMode: Int,
)

