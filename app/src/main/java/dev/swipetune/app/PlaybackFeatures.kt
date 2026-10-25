package dev.swipetune.app

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.core.content.edit

data class PlaybackPreferences(
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Short = 0,
    val bassBoost: Short = 0,
    val virtualizer: Short = 0,
    val loudnessGainMb: Int = 0,
    val replayGainEnabled: Boolean = false,
    val crossfadeSeconds: Int = 0,
    val gaplessEnabled: Boolean = true,
    val offloadEnabled: Boolean = true,
    val sleepDeadlineMs: Long = 0,
)

class PlaybackPreferencesStore(context: Context) {
    private val preferences = context.getSharedPreferences("playback_features", Context.MODE_PRIVATE)

    fun load(): PlaybackPreferences = PlaybackPreferences(
        equalizerEnabled = preferences.getBoolean("eq_enabled", false),
        equalizerPreset = preferences.getInt("eq_preset", 0).toShort(),
        bassBoost = preferences.getInt("bass", 0).toShort(),
        virtualizer = preferences.getInt("virtualizer", 0).toShort(),
        loudnessGainMb = preferences.getInt("loudness", 0),
        replayGainEnabled = preferences.getBoolean("replay_gain", false),
        crossfadeSeconds = preferences.getInt("crossfade", 0),
        gaplessEnabled = preferences.getBoolean("gapless", true),
        offloadEnabled = preferences.getBoolean("offload", true),
        sleepDeadlineMs = preferences.getLong("sleep_deadline", 0),
    )

    fun save(value: PlaybackPreferences) = preferences.edit(commit = true) {
        putBoolean("eq_enabled", value.equalizerEnabled)
        putInt("eq_preset", value.equalizerPreset.toInt())
        putInt("bass", value.bassBoost.toInt())
        putInt("virtualizer", value.virtualizer.toInt())
        putInt("loudness", value.loudnessGainMb)
        putBoolean("replay_gain", value.replayGainEnabled)
        putInt("crossfade", value.crossfadeSeconds)
        putBoolean("gapless", value.gaplessEnabled)
        putBoolean("offload", value.offloadEnabled)
        putLong("sleep_deadline", value.sleepDeadlineMs)
    }
}

class AudioEffectsController {
    private var sessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    fun apply(audioSessionId: Int, preferences: PlaybackPreferences) {
        if (audioSessionId <= 0) return
        if (sessionId != audioSessionId) {
            release()
            sessionId = audioSessionId
        }
        if (!preferences.equalizerEnabled) {
            setEnabled(false)
            return
        }
        runCatching {
            val eq = equalizer ?: Equalizer(0, audioSessionId).also { equalizer = it }
            if (eq.numberOfPresets > 0) eq.usePreset(preferences.equalizerPreset.coerceIn(0, (eq.numberOfPresets - 1).toShort()))
            eq.enabled = true
        }
        runCatching {
            (bassBoost ?: BassBoost(0, audioSessionId).also { bassBoost = it }).apply {
                setStrength(preferences.bassBoost.coerceIn(0, 1000))
                enabled = preferences.bassBoost > 0
            }
        }
        runCatching {
            (virtualizer ?: Virtualizer(0, audioSessionId).also { virtualizer = it }).apply {
                setStrength(preferences.virtualizer.coerceIn(0, 1000))
                enabled = preferences.virtualizer > 0
            }
        }
        runCatching {
            (loudness ?: LoudnessEnhancer(audioSessionId).also { loudness = it }).apply {
                setTargetGain(preferences.loudnessGainMb.coerceIn(0, 2000))
                enabled = preferences.loudnessGainMb > 0
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        sessionId = 0
    }

    private fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { loudness?.enabled = enabled }
    }
}
