package dev.swipetune.app

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val file = AtomicFile(context.filesDir.resolve("swipetune-state.json"))

    fun load(): PersistedState = runCatching {
        if (!file.baseFile.exists()) return PersistedState()
        val root = JSONObject(file.readFully().toString(Charsets.UTF_8))
        val songs = root.optJSONArray("songs").toSongList()
        val decisionsJson = root.optJSONObject("decisions") ?: JSONObject()
        val decisions = buildMap {
            decisionsJson.keys().forEach { uri ->
                runCatching { ReviewDecision.valueOf(decisionsJson.getString(uri)) }
                    .getOrNull()?.let { put(uri, it) }
            }
        }
        val history = root.optJSONArray("history").toHistory()
        PersistedState(
            folderUri = root.optString("folderUri").takeIf(String::isNotBlank),
            songs = songs,
            decisions = decisions,
            swipeOrder = root.optJSONArray("swipeOrder").toStringList(),
            swipeCursor = root.optInt("swipeCursor", 0),
            history = history,
        )
    }.getOrElse { PersistedState() }

    fun save(state: PersistedState) {
        val root = JSONObject().apply {
            put("folderUri", state.folderUri ?: "")
            put("songs", JSONArray().apply {
                state.songs.forEach { song ->
                    put(JSONObject().put("uri", song.uri).put("fileName", song.fileName))
                }
            })
            put("decisions", JSONObject().apply {
                state.decisions.forEach { (uri, decision) -> put(uri, decision.name) }
            })
            put("swipeOrder", JSONArray(state.swipeOrder))
            put("swipeCursor", state.swipeCursor)
            put("history", JSONArray().apply {
                state.history.forEach { entry ->
                    put(JSONObject().put("uri", entry.uri).put("decision", entry.decision.name))
                }
            })
        }
        val output = file.startWrite()
        try {
            output.write(root.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }
}

private fun JSONArray?.toSongList(): List<SongRef> = buildList {
    val array = this@toSongList ?: return@buildList
    repeat(array.length()) { index ->
        val item = array.optJSONObject(index) ?: return@repeat
        val uri = item.optString("uri")
        if (uri.isNotBlank()) add(SongRef(uri, item.optString("fileName", "Unknown track")))
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val array = this@toStringList ?: return@buildList
    repeat(array.length()) { index -> array.optString(index).takeIf(String::isNotBlank)?.let(::add) }
}

private fun JSONArray?.toHistory(): List<ReviewEntry> = buildList {
    val array = this@toHistory ?: return@buildList
    repeat(array.length()) { index ->
        val item = array.optJSONObject(index) ?: return@repeat
        val decision = runCatching { ReviewDecision.valueOf(item.optString("decision")) }.getOrNull()
        val uri = item.optString("uri")
        if (uri.isNotBlank() && decision != null) add(ReviewEntry(uri, decision))
    }
}

