package dev.swipetune.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var playbackStore: PlaybackStore
    private val handler = Handler(Looper.getMainLooper())
    private val persistPosition = object : Runnable {
        override fun run() {
            mediaSession?.player?.let(playbackStore::save)
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
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
        val snapshot = playbackStore.load()
        if (snapshot.items.isNotEmpty()) {
            player.setMediaItems(snapshot.items, snapshot.currentIndex, snapshot.positionMs)
            player.shuffleModeEnabled = snapshot.shuffleEnabled
            player.repeatMode = snapshot.repeatMode
            player.prepare()
        }
        player.addListener(object : androidx.media3.common.Player.Listener {
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
                ) playbackStore.save(player)
            }
        })
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        handler.postDelayed(persistPosition, 5_000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(persistPosition)
        mediaSession?.run {
            playbackStore.save(player)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
