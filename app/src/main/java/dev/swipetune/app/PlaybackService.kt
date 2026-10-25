package dev.swipetune.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import kotlin.math.pow

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var playbackStore: PlaybackStore
    private lateinit var libraryDao: LibraryDao
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioEffects = AudioEffectsController()
    private lateinit var featureStore: PlaybackPreferencesStore
    private var historyTrackUri: String? = null
    private var historyStartedAtMs: Long = 0
    private var historyPlayingSinceMs: Long = 0
    private var historyListenedMs: Long = 0
    private var replayGainVolume = 1f
    private val handler = Handler(Looper.getMainLooper())
    private val persistPosition = object : Runnable {
        override fun run() {
            mediaSession?.player?.let(playbackStore::save)
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        playbackStore = PlaybackStore(this)
        featureStore = PlaybackPreferencesStore(this)
        libraryDao = SwipeTuneDatabase.get(this).libraryDao()
        val snapshot = playbackStore.load()
        applyOffloadPreferences(player, featureStore.load())
        if (snapshot.items.isNotEmpty()) {
            player.setMediaItems(snapshot.items, snapshot.currentIndex, snapshot.positionMs)
            player.shuffleModeEnabled = snapshot.shuffleEnabled
            player.repeatMode = snapshot.repeatMode
            player.prepare()
        }
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (historyTrackUri == null) beginHistory(player.currentMediaItem?.mediaId)
                    historyPlayingSinceMs = System.currentTimeMillis()
                    audioEffects.apply(player.audioSessionId, featureStore.load())
                    scheduleSleepTimer()
                    handler.post(playbackFade)
                } else {
                    accumulateHistoryTime()
                    handler.removeCallbacks(playbackFade)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                finishHistory(completed = reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                beginHistory(mediaItem?.mediaId)
                replayGainVolume = 1f
                player.volume = 1f
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects.apply(audioSessionId, featureStore.load())
            }

            override fun onMetadata(metadata: Metadata) {
                if (!featureStore.load().replayGainEnabled) return
                val gainDb = (0 until metadata.length()).asSequence().mapNotNull { index ->
                    when (val entry = metadata[index]) {
                        is TextInformationFrame -> if (entry.description.equals("REPLAYGAIN_TRACK_GAIN", true)) entry.value.parseGainDb() else null
                        is VorbisComment -> if (entry.key.equals("REPLAYGAIN_TRACK_GAIN", true)) entry.value.parseGainDb() else null
                        else -> null
                    }
                }.firstOrNull() ?: return
                replayGainVolume = kotlin.math.min(1f, 10f.pow(gainDb / 20f))
                player.volume = replayGainVolume
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) finishHistory(completed = true)
            }
            override fun onEvents(
                player: androidx.media3.common.Player,
                events: androidx.media3.common.Player.Events,
            ) {
                if (
                    events.containsAny(
                        androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION,
                        androidx.media3.common.Player.EVENT_TIMELINE_CHANGED,
                        androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        androidx.media3.common.Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        androidx.media3.common.Player.EVENT_REPEAT_MODE_CHANGED,
                        androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY,
                    )
                ) {
                    playbackStore.save(player)
                    serviceScope.launch { PlaybackWidget().updateAll(this@PlaybackService) }
                }
            }
        })
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(sessionActivity)
            .build()
        handler.postDelayed(persistPosition, 5_000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_REFRESH_FEATURES) {
            mediaSession?.player?.let { player ->
                player as ExoPlayer
                val preferences = featureStore.load()
                audioEffects.apply(player.audioSessionId, preferences)
                applyOffloadPreferences(player, preferences)
                if (!preferences.replayGainEnabled) {
                    replayGainVolume = 1f
                    player.volume = 1f
                }
            }
            scheduleSleepTimer()
        }
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> mediaSession?.player?.let { if (it.isPlaying) it.pause() else it.play() }
            ACTION_NEXT -> mediaSession?.player?.seekToNextMediaItem()
            ACTION_PREVIOUS -> mediaSession?.player?.seekToPreviousMediaItem()
        }
        return result
    }

    override fun onDestroy() {
        handler.removeCallbacks(persistPosition)
        handler.removeCallbacks(sleepTimer)
        handler.removeCallbacks(playbackFade)
        finishHistory(completed = false)
        mediaSession?.run {
            playbackStore.save(player)
            player.release()
            release()
        }
        mediaSession = null
        audioEffects.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = completed(
            LibraryResult.ofItem(folder(ROOT, "SwipeTune", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED), params),
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = asyncResult {
            val all = when {
                parentId == ROOT -> rootFolders()
                parentId == SONGS -> libraryDao.tracks().map(::playable)
                parentId == ALBUMS -> libraryDao.albumsOnce().map { folder("album:${it.albumKey}", it.title, MediaMetadata.MEDIA_TYPE_ALBUM) }
                parentId == ARTISTS -> libraryDao.artistsOnce().map { folder("artist:${it.artist}", it.artist, MediaMetadata.MEDIA_TYPE_ARTIST) }
                parentId == GENRES -> libraryDao.genresOnce().map { folder("genre:${it.genre}", it.genre, MediaMetadata.MEDIA_TYPE_GENRE) }
                parentId == FOLDERS -> libraryDao.foldersOnce().map { folder("folder:${it.folderPath}", it.folderName, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED) }
                parentId.startsWith("album:") -> libraryDao.albumTracks(parentId.removePrefix("album:")).map(::playable)
                parentId.startsWith("artist:") -> libraryDao.artistTracks(parentId.removePrefix("artist:")).map(::playable)
                parentId.startsWith("genre:") -> libraryDao.genreTracks(parentId.removePrefix("genre:")).map(::playable)
                parentId.startsWith("folder:") -> libraryDao.folderTracks(parentId.removePrefix("folder:")).map(::playable)
                else -> emptyList()
            }
            val from = (page * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            LibraryResult.ofItemList(all.subList(from, to), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = asyncResult {
            val item = if (mediaId.startsWith("content:") || mediaId.startsWith("file:")) {
                libraryDao.track(mediaId)?.let(::playable)
            } else null
            item?.let { LibraryResult.ofItem(it, null) } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val count = libraryDao.searchTracks(query).size
                session.notifySearchResultChanged(browser, query, count, params)
            }
            return completed(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = asyncResult {
            val all = libraryDao.searchTracks(query).map(::playable)
            val from = (page * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            LibraryResult.ofItemList(all.subList(from, to), params)
        }

        private fun rootFolders(): List<MediaItem> = listOf(
            folder(SONGS, "Songs", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            folder(ALBUMS, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            folder(ARTISTS, "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
            folder(GENRES, "Genres", MediaMetadata.MEDIA_TYPE_FOLDER_GENRES),
            folder(FOLDERS, "Folders", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        )
    }

    private fun folder(id: String, title: String, mediaType: Int): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .build(),
        )
        .build()

    private fun playable(track: TrackEntity): MediaItem = MediaItem.Builder()
        .setMediaId(track.uri)
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artistDisplay)
                .setAlbumTitle(track.albumTitle)
                .setTrackNumber(track.trackNumber.takeIf { it > 0 })
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()

    private fun <T> completed(value: T): ListenableFuture<T> = SettableFuture.create<T>().apply { set(value) }

    private fun <T> asyncResult(block: suspend () -> T): ListenableFuture<T> = SettableFuture.create<T>().also { future ->
        serviceScope.launch {
            runCatching { block() }.onSuccess(future::set).onFailure(future::setException)
        }
    }

    private fun scheduleSleepTimer() {
        handler.removeCallbacks(sleepTimer)
        val deadline = featureStore.load().sleepDeadlineMs
        if (deadline <= 0) return
        val remaining = deadline - System.currentTimeMillis()
        if (remaining <= 0) sleepTimer.run() else handler.postDelayed(sleepTimer, remaining)
    }

    private fun beginHistory(uri: String?) {
        if (uri.isNullOrBlank()) return
        historyTrackUri = uri
        historyStartedAtMs = System.currentTimeMillis()
        historyPlayingSinceMs = if (mediaSession?.player?.isPlaying == true) historyStartedAtMs else 0
        historyListenedMs = 0
    }

    private fun accumulateHistoryTime() {
        if (historyPlayingSinceMs <= 0) return
        historyListenedMs += (System.currentTimeMillis() - historyPlayingSinceMs).coerceAtLeast(0)
        historyPlayingSinceMs = 0
    }

    private fun finishHistory(completed: Boolean) {
        val uri = historyTrackUri ?: return
        accumulateHistoryTime()
        val listened = historyListenedMs
        if (listened >= 10_000 || completed) {
            serviceScope.launch {
                libraryDao.addPlayEvent(
                    PlayEventEntity(
                        trackUri = uri,
                        startedAtMs = historyStartedAtMs,
                        listenedMs = listened,
                        completed = completed,
                        skipped = !completed && listened in 1..29_999,
                        source = "local",
                    ),
                )
            }
        }
        historyTrackUri = null
        historyStartedAtMs = 0
        historyPlayingSinceMs = 0
        historyListenedMs = 0
    }

    private val sleepTimer = Runnable {
        mediaSession?.player?.pause()
        featureStore.save(featureStore.load().copy(sleepDeadlineMs = 0))
    }

    private val playbackFade = object : Runnable {
        override fun run() {
            val player = mediaSession?.player ?: return
            val seconds = featureStore.load().crossfadeSeconds
            val remaining = player.duration - player.currentPosition
            val windowMs = seconds * 1000L
            player.volume = if (seconds > 0 && remaining in 1..windowMs) {
                replayGainVolume * (remaining.toFloat() / windowMs).coerceIn(0f, 1f)
            } else replayGainVolume
            if (player.isPlaying) handler.postDelayed(this, 100)
        }
    }

    private fun applyOffloadPreferences(player: ExoPlayer, preferences: PlaybackPreferences) {
        val mode = if (preferences.offloadEnabled) {
            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
        } else {
            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
        }
        val offload = TrackSelectionParameters.AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(mode)
            .setIsGaplessSupportRequired(preferences.gaplessEnabled)
            .build()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(offload)
            .build()
    }

    companion object {
        const val ACTION_REFRESH_FEATURES = "dev.swipetune.app.REFRESH_PLAYBACK_FEATURES"
        const val ACTION_PLAY_PAUSE = "dev.swipetune.app.PLAY_PAUSE"
        const val ACTION_NEXT = "dev.swipetune.app.NEXT"
        const val ACTION_PREVIOUS = "dev.swipetune.app.PREVIOUS"
        const val ROOT = "root"
        const val SONGS = "songs"
        const val ALBUMS = "albums"
        const val ARTISTS = "artists"
        const val GENRES = "genres"
        const val FOLDERS = "folders"
    }
}

private fun String.parseGainDb(): Float? = Regex("[-+]?\\d+(?:\\.\\d+)?").find(this)?.value?.toFloatOrNull()
