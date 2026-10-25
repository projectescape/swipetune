package dev.swipetune.app

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONArray
import org.json.JSONObject

internal data class PlaybackSnapshot(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

internal class PlaybackStore(context: Context) {
    private val preferences = context.getSharedPreferences("playback", Context.MODE_PRIVATE)

    fun save(player: Player) {
        val items = JSONArray().apply {
            repeat(player.mediaItemCount) { index ->
                val item = player.getMediaItemAt(index)
                val metadata = item.mediaMetadata
                put(
                    JSONObject()
                        .put("id", item.mediaId)
                        .put("title", metadata.title?.toString().orEmpty())
                        .put("artist", metadata.artist?.toString().orEmpty())
                        .put("album", metadata.albumTitle?.toString().orEmpty())
                        .put("track", metadata.trackNumber ?: 0),
                )
            }
        }
        val snapshot = JSONObject()
            .put("items", items)
            .put("index", player.currentMediaItemIndex.coerceAtLeast(0))
            .put("position", player.currentPosition.coerceAtLeast(0))
            .put("shuffle", player.shuffleModeEnabled)
            .put("repeat", player.repeatMode)
        preferences.edit().putString("snapshot", snapshot.toString()).apply()
    }

    fun load(): PlaybackSnapshot = runCatching {
        val root = JSONObject(preferences.getString("snapshot", null) ?: return PlaybackSnapshot())
        val itemJson = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            repeat(itemJson.length()) { index ->
                val item = itemJson.optJSONObject(index) ?: return@repeat
                val id = item.optString("id").takeIf(String::isNotBlank) ?: return@repeat
                add(
                    MediaItem.Builder()
                        .setMediaId(id)
                        .setUri(id)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(item.optString("title"))
                                .setArtist(item.optString("artist"))
                                .setAlbumTitle(item.optString("album"))
                                .setTrackNumber(item.optInt("track", 0))
                                .build(),
                        )
                        .build(),
                )
            }
        }
        PlaybackSnapshot(
            items = items,
            currentIndex = root.optInt("index", 0).coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            positionMs = root.optLong("position", 0).coerceAtLeast(0),
            shuffleEnabled = root.optBoolean("shuffle", false),
            repeatMode = root.optInt("repeat", Player.REPEAT_MODE_OFF),
        )
    }.getOrElse { PlaybackSnapshot() }
}
