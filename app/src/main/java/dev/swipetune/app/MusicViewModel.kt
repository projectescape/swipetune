package dev.swipetune.app

import android.app.Application
import android.content.ComponentName
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SessionStore(application)
    private val repository = MusicRepository(application)
    private val database = SwipeTuneDatabase.get(application)
    private val playbackPreferencesStore = PlaybackPreferencesStore(application)
    private val backupManager = AppBackupManager(application)
    private val lyricsRepository = LyricsRepository(application)
    private val m3uManager = M3uPlaylistManager(application)
    private val _state = MutableStateFlow(store.load().toUiState().copy(playbackPreferences = playbackPreferencesStore.load()))
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    private val controllerFuture: ListenableFuture<MediaController>
    private var player: MediaController? = null
    private var playbackQueue: List<SongRef> = emptyList()
    private var queueBuildJob: Job? = null
    private var queueBuildGeneration = 0
    private var isBuildingQueue = false
    private var queueUndo: QueueSnapshot? = null
    private val loadingArtwork = mutableSetOf<String>()
    private var reviewModeActive = false
    private var normalPlayback: NormalPlaybackSnapshot? = null
    private var observingDeviceLibrary = false
    private var mediaStoreRescanJob: Job? = null
    private var lastForegroundRefreshMs = 0L
    private val sessionSaves = Channel<PersistedState>(Channel.CONFLATED)
    private val _deleteRequests = Channel<IntentSender>(Channel.BUFFERED)
    val deleteRequests = _deleteRequests.receiveAsFlow()
    private var pendingDeletion: PendingDeletion? = null
    private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            mediaStoreRescanJob?.cancel()
            mediaStoreRescanJob = viewModelScope.launch {
                delay(700)
                scanDeviceLibrary(announce = false)
            }
        }
    }

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
                if (!isBuildingQueue) syncQueueFromPlayer()
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
        viewModelScope.launch(Dispatchers.IO) {
            for (persisted in sessionSaves) store.save(persisted)
        }
        viewModelScope.launch(Dispatchers.IO) {
            LegacyStateMigrator(database).migrate(store.load())
        }
        viewModelScope.launch {
            database.libraryDao().observeRecent().collect { history ->
                _state.update { it.copy(recentHistory = history) }
            }
        }
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
            _state.update { it.copy(isScanning = true, errorMessage = null, statusMessage = "Adding folder…") }
            runCatching {
                val songs = withContext(Dispatchers.IO) { repository.scanAndIndex(uri) }
                mergeScannedLibrary(songs, uri.toString())
            }.onSuccess { persisted ->
                saveAndShow(persisted)
                _state.update {
                    it.copy(isScanning = false, statusMessage = "Library updated · ${persisted.songs.size} songs")
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isScanning = false, statusMessage = null, errorMessage = error.message ?: "Could not scan folder")
                }
            }
        }
    }

    fun scanDeviceLibrary(announce: Boolean = true) {
        if (_state.value.isScanning) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isScanning = true,
                    errorMessage = null,
                    statusMessage = if (announce) "Indexing device music…" else null,
                )
            }
            runCatching {
                val songs = withContext(Dispatchers.IO) {
                    _state.value.folderUri?.let { repository.scanAndIndex(Uri.parse(it)) } ?: repository.scanDevice()
                }
                mergeScannedLibrary(songs, _state.value.folderUri)
            }.onSuccess { persisted ->
                saveAndShow(persisted)
                _state.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = if (announce) "Indexed ${persisted.songs.size} songs" else null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isScanning = false, statusMessage = null, errorMessage = error.message ?: "Could not index device music")
                }
            }
        }
    }

    fun onAppForeground() {
        startObservingDeviceLibrary()
        val now = System.currentTimeMillis()
        if (_state.value.songs.isEmpty()) {
            lastForegroundRefreshMs = now
            scanDeviceLibrary(announce = true)
        } else if (now - lastForegroundRefreshMs >= FOREGROUND_REFRESH_COOLDOWN_MS) {
            lastForegroundRefreshMs = now
            scanDeviceLibrary(announce = false)
        }
    }

    fun startObservingDeviceLibrary() {
        if (observingDeviceLibrary) return
        val resolver = getApplication<Application>().contentResolver
        resolver.registerContentObserver(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), true, mediaStoreObserver)
        resolver.registerContentObserver(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), true, mediaStoreObserver)
        observingDeviceLibrary = true
    }

    fun stopObservingDeviceLibrary() {
        if (!observingDeviceLibrary) return
        mediaStoreRescanJob?.cancel()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        observingDeviceLibrary = false
    }

    fun onAudioPermissionDenied() {
        _state.update {
            it.copy(errorMessage = "Music access is off. Grant audio permission in Android settings, or add a folder from SwipeTune settings.")
        }
    }

    fun prepareReview(folderName: String?) {
        val current = _state.value.toPersisted()
        val candidates = current.songs.filter { folderName == null || it.folderName == folderName }
        val sameScope = current.reviewFolderName == folderName
        if (sameScope) {
            val validOrder = current.swipeOrder.filter { uri -> candidates.any { it.uri == uri } }
            val additions = ReviewQueue.create(candidates, current.decisions).filterNot(validOrder::contains)
            saveAndShow(current.copy(swipeOrder = validOrder + additions, swipeCursor = current.swipeCursor.coerceAtMost(validOrder.size)))
        } else {
            saveAndShow(
                current.copy(
                    reviewFolderName = folderName,
                    swipeOrder = ReviewQueue.create(candidates, current.decisions),
                    swipeCursor = 0,
                    history = emptyList(),
                ),
            )
        }
    }

    fun clearReviewDecision(song: SongRef) {
        val current = _state.value.toPersisted()
        val nextDecisions = current.decisions - song.uri
        val belongsToScope = current.reviewFolderName == null || song.folderName == current.reviewFolderName
        val reviewed = current.swipeOrder.take(current.swipeCursor).filterNot { it == song.uri }
        val pending = current.swipeOrder.drop(current.swipeCursor).filterNot { it == song.uri }
        saveAndShow(
            current.copy(
                decisions = nextDecisions,
                swipeOrder = reviewed + pending + if (belongsToScope) listOf(song.uri) else emptyList(),
                swipeCursor = reviewed.size,
                history = current.history.filterNot { it.uri == song.uri },
            ),
        )
    }

    fun setReviewDecision(song: SongRef, decision: ReviewDecision?) {
        if (decision == null) {
            clearReviewDecision(song)
            _state.update { it.copy(statusMessage = "Review decision cleared") }
            return
        }
        val current = _state.value.toPersisted()
        val reviewed = current.swipeOrder.take(current.swipeCursor).filterNot { it == song.uri }
        val pending = current.swipeOrder.drop(current.swipeCursor).filterNot { it == song.uri }
        saveAndShow(
            current.copy(
                decisions = current.decisions + (song.uri to decision),
                swipeOrder = reviewed + pending,
                swipeCursor = reviewed.size,
                history = current.history + ReviewEntry(song.uri, decision),
            ),
        )
        _state.update {
            it.copy(statusMessage = if (decision == ReviewDecision.KEPT) "Marked accepted" else "Marked rejected")
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
        val visibleUris = _state.value.visibleSongs.mapTo(hashSetOf(), SongRef::uri)
        val restoredItems = snapshot?.items.orEmpty().filter { it.mediaId in visibleUris }
        if (snapshot == null || restoredItems.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            clearNowPlaying()
            return
        }
        val previousUri = snapshot.items.getOrNull(snapshot.index)?.mediaId
        val restoredIndex = restoredItems.indexOfFirst { it.mediaId == previousUri }.takeIf { it >= 0 } ?: 0
        val restoredPosition = snapshot.positionMs.takeIf { restoredItems[restoredIndex].mediaId == previousUri } ?: 0L
        player.setMediaItems(restoredItems, restoredIndex, restoredPosition)
        player.shuffleModeEnabled = snapshot.shuffle
        player.repeatMode = snapshot.repeatMode
        player.prepare()
        player.playWhenReady = snapshot.playWhenReady
        syncQueueFromPlayer()
        restoredItems.getOrNull(restoredIndex)?.mediaId?.let { uri ->
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
        val player = player?.takeIf(Player::hasNextMediaItem) ?: return
        showSongPreview(player.nextMediaItemIndex)
        player.seekToNextMediaItem()
    }

    fun previousVisibleSong() {
        val player = player ?: return
        if (player.currentPosition > 3_000) player.seekTo(0)
        else if (player.hasPreviousMediaItem()) {
            showSongPreview(player.previousMediaItemIndex)
            player.seekToPreviousMediaItem()
        }
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
        cancelQueueBuild()
        val player = player ?: return
        val index = (0 until player.mediaItemCount).firstOrNull {
            player.getMediaItemAt(it).mediaId == uri
        } ?: return
        rememberQueueForUndo(player)
        if (player.mediaItemCount == 1) {
            player.stop()
            player.clearMediaItems()
            clearNowPlaying()
        } else {
            player.removeMediaItem(index)
            syncQueueFromPlayer()
        }
    }

    fun playNext(songs: List<SongRef>) {
        cancelQueueBuild()
        val player = player ?: return
        if (songs.isEmpty()) return
        rememberQueueForUndo(player)
        val insertAt = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItems(insertAt, songs.map(::mediaItem))
        syncQueueFromPlayer()
    }

    fun appendToQueue(songs: List<SongRef>) {
        cancelQueueBuild()
        val player = player ?: return
        if (songs.isEmpty()) return
        rememberQueueForUndo(player)
        player.addMediaItems(songs.map(::mediaItem))
        syncQueueFromPlayer()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        cancelQueueBuild()
        val player = player ?: return
        if (fromIndex !in 0 until player.mediaItemCount || toIndex !in 0 until player.mediaItemCount || fromIndex == toIndex) return
        rememberQueueForUndo(player)
        player.moveMediaItem(fromIndex, toIndex)
        syncQueueFromPlayer()
    }

    fun clearUpcomingQueue() {
        cancelQueueBuild()
        val player = player ?: return
        val current = player.currentMediaItemIndex
        if (current < 0 || player.mediaItemCount <= 1) return
        rememberQueueForUndo(player)
        if (current + 1 < player.mediaItemCount) player.removeMediaItems(current + 1, player.mediaItemCount)
        if (current > 0) player.removeMediaItems(0, current)
        syncQueueFromPlayer()
    }

    fun undoQueueEdit() {
        cancelQueueBuild()
        val player = player ?: return
        val snapshot = queueUndo ?: return
        queueUndo = null
        player.setMediaItems(snapshot.items, snapshot.index.coerceIn(0, (snapshot.items.size - 1).coerceAtLeast(0)), snapshot.positionMs)
        player.prepare()
        player.playWhenReady = snapshot.playWhenReady
        syncQueueFromPlayer()
    }

    fun saveQueueAsPlaylist(name: String) {
        val cleanName = name.trim()
        val player = player ?: return
        if (cleanName.isBlank() || player.mediaItemCount == 0) return
        val uris = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }.filter(String::isNotBlank)
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(playlists = current.playlists + UserPlaylist(UUID.randomUUID().toString(), cleanName, uris)),
        )
    }

    fun toggleSongSelection(song: SongRef) = _state.update { current ->
        current.copy(
            selectedSongUris = if (song.uri in current.selectedSongUris) {
                current.selectedSongUris - song.uri
            } else {
                current.selectedSongUris + song.uri
            },
        )
    }

    fun clearSongSelection() = _state.update { it.copy(selectedSongUris = emptySet()) }

    fun playSelectedNext() {
        playNext(_state.value.selectedSongs())
        clearSongSelection()
    }

    fun appendSelected() {
        appendToQueue(_state.value.selectedSongs())
        clearSongSelection()
    }

    fun favoriteSelected() {
        val selected = _state.value.selectedSongUris
        if (selected.isEmpty()) return
        val current = _state.value.toPersisted()
        saveAndShow(current.copy(favorites = current.favorites + selected))
        clearSongSelection()
    }

    fun rejectSelected() {
        val selected = _state.value.selectedSongUris
        if (selected.isEmpty()) return
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(
                decisions = current.decisions + selected.associateWith { ReviewDecision.REJECTED },
                history = current.history + selected.map { ReviewEntry(it, ReviewDecision.REJECTED) },
            ),
        )
        clearSongSelection()
    }

    fun findDuplicates() {
        viewModelScope.launch(Dispatchers.Default) {
            val groups = DuplicateFinder.find(_state.value.visibleSongs)
            _state.update { it.copy(duplicateGroups = groups, statusMessage = "Found ${groups.size} duplicate groups") }
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
        if (id == ACCEPTED_PLAYLIST_ID || id == REJECTED_PLAYLIST_ID) return
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

    fun movePlaylistSong(playlistId: String, fromIndex: Int, toIndex: Int) {
        val current = _state.value.toPersisted()
        saveAndShow(current.copy(playlists = current.playlists.map { playlist ->
            if (playlist.id != playlistId || fromIndex !in playlist.songUris.indices || toIndex !in playlist.songUris.indices) playlist
            else playlist.copy(songUris = playlist.songUris.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
        }))
    }

    fun duplicatePlaylist(playlistId: String) {
        val current = _state.value.toPersisted()
        val source = current.playlists.firstOrNull { it.id == playlistId } ?: return
        saveAndShow(
            current.copy(
                playlists = current.playlists + source.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${source.name} copy",
                ),
            ),
        )
    }

    fun quickFillPlaylist(playlistId: String) {
        val current = _state.value.toPersisted()
        val source = current.playlists.firstOrNull { it.id == playlistId } ?: return
        val existingSongs = source.songUris.mapNotNull { uri -> current.songs.firstOrNull { it.uri == uri } }
        val artists = existingSongs.map { it.artist }.toSet()
        val albums = existingSongs.map { it.album }.toSet()
        val additions = current.songs.asSequence()
            .filter { it.uri !in source.songUris && (it.artist in artists || it.album in albums) }
            .map(SongRef::uri)
            .take(25)
            .toList()
        saveAndShow(current.copy(playlists = current.playlists.map {
            if (it.id == playlistId) it.copy(songUris = it.songUris + additions) else it
        }))
    }

    fun mergePlaylist(targetId: String, sourceId: String) {
        val current = _state.value.toPersisted()
        val source = current.playlists.firstOrNull { it.id == sourceId } ?: return
        saveAndShow(current.copy(playlists = current.playlists.map { target ->
            if (target.id == targetId) target.copy(songUris = (target.songUris + source.songUris).distinct()) else target
        }))
    }

    fun setPlaylistCover(playlistId: String, uri: Uri) {
        val current = _state.value.toPersisted()
        saveAndShow(current.copy(playlists = current.playlists.map {
            if (it.id == playlistId) it.copy(coverUri = uri.toString()) else it
        }))
    }

    fun exportPlaylist(playlistId: String, destination: Uri) {
        val state = _state.value
        val playlist = state.playlists.firstOrNull { it.id == playlistId } ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { m3uManager.export(destination, playlist, state.songs) } }
                .onSuccess { _state.update { it.copy(statusMessage = "Playlist exported") } }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message ?: "Could not export playlist") } }
        }
    }

    fun importPlaylist(source: Uri) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { m3uManager.import(source, _state.value.songs) } }
                .onSuccess { (name, uris) ->
                    val current = _state.value.toPersisted()
                    saveAndShow(current.copy(playlists = current.playlists + UserPlaylist(UUID.randomUUID().toString(), name, uris)))
                    _state.update { it.copy(statusMessage = "Imported ${uris.size} songs") }
                }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message ?: "Could not import playlist") } }
        }
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

    fun setArtworkAtmosphere(enabled: Boolean) = saveAndShow(_state.value.toPersisted().copy(artworkAtmosphere = enabled))
    fun setExpressiveMotion(enabled: Boolean) = saveAndShow(_state.value.toPersisted().copy(expressiveMotion = enabled))
    fun setCompactLibraryRows(enabled: Boolean) = saveAndShow(_state.value.toPersisted().copy(compactLibraryRows = enabled))

    fun updatePlaybackPreferences(transform: (PlaybackPreferences) -> PlaybackPreferences) {
        val updated = transform(_state.value.playbackPreferences)
        playbackPreferencesStore.save(updated)
        _state.update { it.copy(playbackPreferences = updated) }
        getApplication<Application>().startService(
            android.content.Intent(getApplication(), PlaybackService::class.java).setAction(PlaybackService.ACTION_REFRESH_FEATURES),
        )
    }

    fun setSleepTimer(minutes: Int) {
        updatePlaybackPreferences {
            it.copy(sleepDeadlineMs = if (minutes <= 0) 0 else System.currentTimeMillis() + minutes * 60_000L)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(statusMessage = "Creating backup…", errorMessage = null) }
            runCatching { withContext(Dispatchers.IO) { backupManager.export(uri) } }
                .onSuccess { _state.update { it.copy(statusMessage = "Backup saved") } }
                .onFailure { error -> _state.update { it.copy(statusMessage = null, errorMessage = error.message ?: "Could not create backup") } }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(statusMessage = "Checking backup…", errorMessage = null) }
            runCatching { withContext(Dispatchers.IO) { backupManager.restore(uri) } }
                .onSuccess { restored ->
                    saveAndShow(restored)
                    _state.update {
                        it.copy(
                            playbackPreferences = playbackPreferencesStore.load(),
                            statusMessage = "Backup restored",
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(statusMessage = null, errorMessage = error.message ?: "Could not restore backup") } }
        }
    }

    fun importLyrics(uri: Uri) {
        val trackUri = _state.value.nowPlayingUri ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { lyricsRepository.import(trackUri, uri) } }
                .onSuccess { lines -> _state.update { it.copy(lyrics = lines, statusMessage = "Lyrics imported") } }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message ?: "Could not import lyrics") } }
        }
    }

    fun rescanLibrary() {
        scanDeviceLibrary()
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

    fun requestDeleteRejected() = requestDeleteSongs(_state.value.rejectedSongs)

    fun requestDeleteSong(song: SongRef) = requestDeleteSongs(listOf(song))

    private fun requestDeleteSongs(songs: List<SongRef>) {
        if (_state.value.isDeleting || songs.isEmpty()) return
        viewModelScope.launch {
            val distinct = songs.distinctBy(SongRef::uri)
            _state.update { it.copy(isDeleting = true, statusMessage = "Preparing deletion…", errorMessage = null) }
            val direct = withContext(Dispatchers.IO) {
                distinct.filter(repository::canDeleteDirectly).filter(repository::deleteDocument)
            }
            val directlyDeletedUris = direct.mapTo(hashSetOf(), SongRef::uri)
            val remaining = distinct.filterNot { it.uri in directlyDeletedUris }
            if (remaining.isEmpty()) {
                finishDeletion(direct, distinct.size)
                return@launch
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = runCatching {
                    MediaStore.createDeleteRequest(
                        getApplication<Application>().contentResolver,
                        remaining.map { Uri.parse(it.uri) },
                    ).intentSender
                }.getOrNull()
                if (intentSender != null) {
                    pendingDeletion = PendingDeletion(remaining, direct, distinct.size)
                    _state.update { it.copy(statusMessage = "Confirm deletion with Android") }
                    _deleteRequests.send(intentSender)
                    return@launch
                }
            }

            val legacyDeleted = withContext(Dispatchers.IO) { remaining.filter(repository::deleteMediaStore) }
            finishDeletion(direct + legacyDeleted, distinct.size)
        }
    }

    fun completeDeleteRequest(approved: Boolean) {
        val pending = pendingDeletion ?: return
        pendingDeletion = null
        if (approved) {
            finishDeletion(pending.directlyDeleted + pending.awaitingApproval, pending.requestedCount)
        } else {
            finishDeletion(pending.directlyDeleted, pending.requestedCount, canceled = true)
        }
    }

    private fun finishDeletion(deleted: List<SongRef>, requestedCount: Int, canceled: Boolean = false) {
        val deletedUris = deleted.mapTo(hashSetOf(), SongRef::uri)
        val deletedCurrentTrack = _state.value.nowPlayingUri in deletedUris
        val current = _state.value.toPersisted()
        saveAndShow(
            current.copy(
                songs = current.songs.filterNot { it.uri in deletedUris },
                decisions = current.decisions.filterKeys { it !in deletedUris },
                swipeOrder = current.swipeOrder.filterNot { it in deletedUris },
                history = current.history.filterNot { it.uri in deletedUris },
                favorites = current.favorites.filterNot { it in deletedUris }.toSet(),
                playlists = current.playlists.map { playlist ->
                    playlist.copy(songUris = playlist.songUris.filterNot { it in deletedUris })
                },
            ),
        )
        if (deletedCurrentTrack) {
            player?.stop()
            player?.clearMediaItems()
            clearNowPlaying()
        }
        _state.update {
            it.copy(
                isDeleting = false,
                statusMessage = when {
                    canceled && deleted.isEmpty() -> "Deletion canceled"
                    canceled -> "Deleted ${deleted.size} files; remaining deletion canceled"
                    deleted.size == requestedCount -> "Deleted ${deleted.size} ${if (deleted.size == 1) "file" else "files"}"
                    else -> "Deleted ${deleted.size} of $requestedCount files"
                },
            )
        }
    }

    fun clearMessage() = _state.update { it.copy(statusMessage = null, errorMessage = null) }

    private fun saveAndShow(persisted: PersistedState) {
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                database.libraryDao().clearReviewDecisions()
                if (persisted.decisions.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    database.libraryDao().putReviewDecisions(
                        persisted.decisions.map { (uri, decision) -> ReviewDecisionEntity(uri, decision.name, now) },
                    )
                }
            }
        }
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
            playbackPreferences = playback.playbackPreferences,
            recentHistory = playback.recentHistory,
            lyrics = playback.lyrics,
            selectedSongUris = playback.selectedSongUris,
            duplicateGroups = playback.duplicateGroups,
        )
        sessionSaves.trySend(persisted)
    }

    private fun mergeScannedLibrary(songs: List<SongRef>, folderUri: String?): PersistedState {
        val old = _state.value
        val validUris = songs.mapTo(hashSetOf(), SongRef::uri)
        val decisions = old.decisions.filterKeys(validUris::contains)
        val scopedSongs = songs.filter { old.reviewFolderName == null || it.folderName == old.reviewFolderName }
        val scopedUris = scopedSongs.mapTo(hashSetOf(), SongRef::uri)
        val reviewedPrefix = old.swipeOrder.take(old.swipeCursor).filter(validUris::contains)
        val remaining = old.swipeOrder.drop(old.swipeCursor)
            .filter { it in validUris && it !in decisions && it in scopedUris }
        val additions = ReviewQueue.create(scopedSongs, decisions).filterNot(remaining::contains)
        return old.toPersisted().copy(
            folderUri = folderUri,
            songs = songs,
            decisions = decisions,
            swipeOrder = reviewedPrefix + remaining + additions,
            swipeCursor = reviewedPrefix.size.coerceAtMost(reviewedPrefix.size + remaining.size + additions.size),
            history = old.history.filter { it.uri in validUris },
            favorites = old.favorites.intersect(validUris),
            playlists = old.playlists.map { playlist ->
                playlist.copy(songUris = playlist.songUris.filter(validUris::contains))
            },
        )
    }

    private fun playQueue(songs: List<SongRef>, start: SongRef, autoplay: Boolean) {
        if (songs.isEmpty()) return
        val player = player ?: run {
            _state.update { it.copy(errorMessage = "The player is still starting. Try again in a moment.") }
            return
        }
        queueBuildJob?.cancel()
        val generation = ++queueBuildGeneration
        playbackQueue = songs
        val startIndex = songs.indexOfFirst { it.uri == start.uri }.coerceAtLeast(0)
        val queueUris = songs.map(SongRef::uri)
        _state.update {
            it.copy(
                nowPlayingUri = start.uri,
                metadata = TrackMetadata(start.title, start.artist, start.album, it.artworkByUri[start.uri]),
                positionMs = 0,
                durationMs = start.durationMs,
                playbackQueue = queueUris,
                errorMessage = null,
            )
        }

        val immediateAfterEnd = (startIndex + 1 + QUEUE_START_WINDOW).coerceAtMost(songs.size)
        player.setMediaItem(mediaItem(start))
        if (startIndex + 1 < immediateAfterEnd) {
            player.addMediaItems(songs.subList(startIndex + 1, immediateAfterEnd).map(::mediaItem))
        }
        player.prepare()
        player.playWhenReady = autoplay
        showCurrentSong(start)

        val hasDeferredItems = startIndex > 0 || immediateAfterEnd < songs.size
        isBuildingQueue = hasDeferredItems
        if (!hasDeferredItems) return

        queueBuildJob = viewModelScope.launch {
            try {
                // Let the selected row, artwork, and transport state settle before
                // sending the rest of a very large queue across the media session.
                delay(500)
                var cursor = immediateAfterEnd
                while (cursor < songs.size && generation == queueBuildGeneration) {
                    val end = (cursor + QUEUE_CHUNK_SIZE).coerceAtMost(songs.size)
                    val items = withContext(Dispatchers.Default) { songs.subList(cursor, end).map(::mediaItem) }
                    player.addMediaItems(items)
                    cursor = end
                    delay(64)
                }

                var beforeEnd = startIndex
                while (beforeEnd > 0 && generation == queueBuildGeneration) {
                    val beforeStart = (beforeEnd - QUEUE_CHUNK_SIZE).coerceAtLeast(0)
                    val items = withContext(Dispatchers.Default) { songs.subList(beforeStart, beforeEnd).map(::mediaItem) }
                    player.addMediaItems(0, items)
                    beforeEnd = beforeStart
                    delay(64)
                }
            } finally {
                if (generation == queueBuildGeneration) {
                    isBuildingQueue = false
                    _state.update { it.copy(playbackQueue = queueUris) }
                }
            }
        }
    }

    private fun cancelQueueBuild() {
        queueBuildGeneration++
        queueBuildJob?.cancel()
        queueBuildJob = null
        isBuildingQueue = false
    }

    private fun showCurrentSong(song: SongRef) {
        _state.update {
            it.copy(
                nowPlayingUri = song.uri,
                metadata = TrackMetadata(song.title, song.artist, song.album, it.artworkByUri[song.uri]),
                lyrics = emptyList(),
            )
        }
        viewModelScope.launch {
            val metadata = runCatching { withContext(Dispatchers.IO) { repository.metadata(song) } }
                .getOrElse { TrackMetadata(song.title, song.artist, song.album) }
            val lyrics = withContext(Dispatchers.IO) { lyricsRepository.load(song.uri) }
            if (_state.value.nowPlayingUri == song.uri) {
                _state.update { current ->
                    current.copy(
                        metadata = metadata,
                        artworkByUri = metadata.artwork?.let { current.artworkByUri + (song.uri to it) }
                            ?: current.artworkByUri,
                        lyrics = lyrics,
                    )
                }
            }
        }
    }

    private fun showSongPreview(index: Int) {
        val player = player ?: return
        if (index !in 0 until player.mediaItemCount) return
        val uri = player.getMediaItemAt(index).mediaId
        val song = _state.value.songs.firstOrNull { it.uri == uri } ?: return
        _state.update {
            it.copy(
                nowPlayingUri = song.uri,
                metadata = TrackMetadata(song.title, song.artist, song.album, it.artworkByUri[song.uri]),
                positionMs = 0,
                durationMs = song.durationMs,
                lyrics = emptyList(),
            )
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
        cancelQueueBuild()
        _state.update {
            it.copy(
                nowPlayingUri = null,
                metadata = null,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
                playbackQueue = emptyList(),
                lyrics = emptyList(),
            )
        }
    }

    override fun onCleared() {
        cancelQueueBuild()
        stopObservingDeviceLibrary()
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

    private fun mediaItem(song: SongRef): MediaItem = MediaItem.Builder()
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

    private fun rememberQueueForUndo(player: Player) {
        queueUndo = QueueSnapshot(
            items = List(player.mediaItemCount, player::getMediaItemAt),
            index = player.currentMediaItemIndex.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0),
            playWhenReady = player.playWhenReady,
        )
    }
}

private const val QUEUE_START_WINDOW = 32
private const val QUEUE_CHUNK_SIZE = 128
private const val FOREGROUND_REFRESH_COOLDOWN_MS = 60_000L

private data class PendingDeletion(
    val awaitingApproval: List<SongRef>,
    val directlyDeleted: List<SongRef>,
    val requestedCount: Int,
)

private fun PersistedState.toUiState() = AppUiState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor.coerceIn(0, swipeOrder.size),
    history = history,
    reviewFolderName = reviewFolderName,
    favorites = favorites,
    playlists = playlists,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    artworkAtmosphere = artworkAtmosphere,
    expressiveMotion = expressiveMotion,
    compactLibraryRows = compactLibraryRows,
)

private fun AppUiState.toPersisted() = PersistedState(
    folderUri = folderUri,
    songs = songs,
    decisions = decisions,
    swipeOrder = swipeOrder,
    swipeCursor = swipeCursor,
    history = history,
    reviewFolderName = reviewFolderName,
    favorites = favorites,
    playlists = playlists,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    artworkAtmosphere = artworkAtmosphere,
    expressiveMotion = expressiveMotion,
    compactLibraryRows = compactLibraryRows,
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

private data class QueueSnapshot(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
    val playWhenReady: Boolean,
)

private fun AppUiState.selectedSongs(): List<SongRef> = visibleSongs.filter { it.uri in selectedSongUris }

