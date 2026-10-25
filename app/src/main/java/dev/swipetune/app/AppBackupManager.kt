package dev.swipetune.app

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppBackupManager(
    private val context: Context,
    private val database: SwipeTuneDatabase = SwipeTuneDatabase.get(context),
) {
    suspend fun export(destination: Uri) {
        val state = SessionStore(context).exportJson()
        val preferences = PlaybackPreferencesStore(context).load()
        val events = database.libraryDao().playEventsOnce()
        val manifest = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAtMs", System.currentTimeMillis())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("playback", preferences.toJson())
            .put("events", JSONArray().apply { events.forEach { put(it.toJson()) } })
        context.contentResolver.openOutputStream(destination, "w")?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("state.json"))
                zip.write(state.toByteArray())
                zip.closeEntry()
            }
        } ?: error("Could not open the backup destination")
    }

    suspend fun restore(source: Uri): PersistedState {
        var manifestText: String? = null
        var stateText: String? = null
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "manifest.json" -> manifestText = zip.readBytes().toString(Charsets.UTF_8)
                        "state.json" -> stateText = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Could not open this backup")
        val manifest = JSONObject(manifestText ?: error("Backup manifest is missing"))
        require(manifest.optString("format") == FORMAT) { "This is not a SwipeTune backup" }
        require(manifest.optInt("version") <= VERSION) { "This backup was created by a newer SwipeTune version" }
        val stateJson = stateText ?: error("Backup state is missing")
        JSONObject(stateJson)
        val eventsJson = manifest.optJSONArray("events") ?: JSONArray()
        val events = buildList {
            repeat(eventsJson.length()) { index -> eventsJson.optJSONObject(index)?.toPlayEvent()?.let(::add) }
        }
        val restored = SessionStore(context).importJson(stateJson)
        manifest.optJSONObject("playback")?.toPlaybackPreferences()?.let(PlaybackPreferencesStore(context)::save)
        database.withTransaction {
            database.libraryDao().clearPlayEvents()
            if (events.isNotEmpty()) database.libraryDao().addPlayEvents(events.map { it.copy(id = 0) })
            database.libraryDao().putMarker(AppMarkerEntity("last_explicit_restore", System.currentTimeMillis().toString()))
        }
        return restored
    }

    private fun PlaybackPreferences.toJson() = JSONObject()
        .put("equalizerEnabled", equalizerEnabled)
        .put("equalizerPreset", equalizerPreset.toInt())
        .put("bassBoost", bassBoost.toInt())
        .put("virtualizer", virtualizer.toInt())
        .put("loudnessGainMb", loudnessGainMb)
        .put("replayGainEnabled", replayGainEnabled)
        .put("crossfadeSeconds", crossfadeSeconds)
        .put("gaplessEnabled", gaplessEnabled)
        .put("offloadEnabled", offloadEnabled)

    private fun JSONObject.toPlaybackPreferences() = PlaybackPreferences(
        equalizerEnabled = optBoolean("equalizerEnabled"),
        equalizerPreset = optInt("equalizerPreset").toShort(),
        bassBoost = optInt("bassBoost").toShort(),
        virtualizer = optInt("virtualizer").toShort(),
        loudnessGainMb = optInt("loudnessGainMb"),
        replayGainEnabled = optBoolean("replayGainEnabled"),
        crossfadeSeconds = optInt("crossfadeSeconds"),
        gaplessEnabled = optBoolean("gaplessEnabled", true),
        offloadEnabled = optBoolean("offloadEnabled", true),
    )

    private fun PlayEventEntity.toJson() = JSONObject()
        .put("trackUri", trackUri)
        .put("startedAtMs", startedAtMs)
        .put("listenedMs", listenedMs)
        .put("completed", completed)
        .put("skipped", skipped)
        .put("source", source)

    private fun JSONObject.toPlayEvent(): PlayEventEntity? {
        val uri = optString("trackUri").takeIf(String::isNotBlank) ?: return null
        return PlayEventEntity(
            trackUri = uri,
            startedAtMs = optLong("startedAtMs"),
            listenedMs = optLong("listenedMs"),
            completed = optBoolean("completed"),
            skipped = optBoolean("skipped"),
            source = optString("source", "local"),
        )
    }

    private companion object {
        const val FORMAT = "dev.swipetune.backup"
        const val VERSION = 1
    }
}
