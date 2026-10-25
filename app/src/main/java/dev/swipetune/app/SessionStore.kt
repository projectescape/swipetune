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
            reviewFolderName = root.optString("reviewFolderName").takeIf(String::isNotBlank),
            favorites = root.optJSONArray("favorites").toStringList().toSet(),
            playlists = root.optJSONArray("playlists").toPlaylists(),
            themeMode = runCatching { ThemeMode.valueOf(root.optString("themeMode", ThemeMode.SYSTEM.name)) }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = root.optBoolean("dynamicColor", true),
            artworkAtmosphere = root.optBoolean("artworkAtmosphere", true),
            expressiveMotion = root.optBoolean("expressiveMotion", true),
            compactLibraryRows = root.optBoolean("compactLibraryRows", false),
        )
    }.getOrElse { PersistedState() }

    fun save(state: PersistedState) {
        val root = JSONObject().apply {
            put("folderUri", state.folderUri ?: "")
            put("songs", JSONArray().apply {
                state.songs.forEach { song ->
                    put(JSONObject().apply {
                        put("uri", song.uri)
                        put("fileName", song.fileName)
                        put("title", song.title)
                        put("artist", song.artist)
                        put("album", song.album)
                        put("folderName", song.folderName)
                        put("genre", song.genre)
                        put("durationMs", song.durationMs)
                        put("trackNumber", song.trackNumber)
                    })
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
            put("reviewFolderName", state.reviewFolderName ?: "")
            put("favorites", JSONArray(state.favorites.toList()))
            put("playlists", JSONArray().apply {
                state.playlists.forEach { playlist ->
                    put(
                        JSONObject()
                            .put("id", playlist.id)
                            .put("name", playlist.name)
                            .put("songs", JSONArray(playlist.songUris))
                            .put("coverUri", playlist.coverUri ?: ""),
                    )
                }
            })
            put("themeMode", state.themeMode.name)
            put("dynamicColor", state.dynamicColor)
            put("artworkAtmosphere", state.artworkAtmosphere)
            put("expressiveMotion", state.expressiveMotion)
            put("compactLibraryRows", state.compactLibraryRows)
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

    fun exportJson(): String = if (file.baseFile.exists()) {
        file.readFully().toString(Charsets.UTF_8)
    } else {
        save(PersistedState())
        file.readFully().toString(Charsets.UTF_8)
    }

    fun importJson(json: String): PersistedState {
        JSONObject(json)
        val output = file.startWrite()
        try {
            output.write(json.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
        return load()
    }
}

private fun JSONArray?.toSongList(): List<SongRef> = buildList {
    val array = this@toSongList ?: return@buildList
    repeat(array.length()) { index ->
        val item = array.optJSONObject(index) ?: return@repeat
        val uri = item.optString("uri")
        if (uri.isNotBlank()) add(
            SongRef(
                uri = uri,
                fileName = item.optString("fileName", "Unknown track"),
                title = item.optString("title").takeIf(String::isNotBlank)
                    ?: item.optString("fileName", "Unknown track").substringBeforeLast('.'),
                artist = item.optString("artist", "Unknown artist"),
                album = item.optString("album", "Unknown album"),
                folderName = item.optString("folderName", "Music"),
                genre = item.optString("genre", "Uncategorized"),
                durationMs = item.optLong("durationMs", 0),
                trackNumber = item.optInt("trackNumber", 0),
            ),
        )
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

private fun JSONArray?.toPlaylists(): List<UserPlaylist> = buildList {
    val array = this@toPlaylists ?: return@buildList
    repeat(array.length()) { index ->
        val item = array.optJSONObject(index) ?: return@repeat
        val id = item.optString("id").takeIf(String::isNotBlank) ?: return@repeat
        val name = item.optString("name").takeIf(String::isNotBlank) ?: return@repeat
        add(UserPlaylist(id, name, item.optJSONArray("songs").toStringList(), item.optString("coverUri").takeIf(String::isNotBlank)))
    }
}

