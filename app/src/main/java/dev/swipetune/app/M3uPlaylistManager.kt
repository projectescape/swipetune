package dev.swipetune.app

import android.content.Context
import android.net.Uri

class M3uPlaylistManager(private val context: Context) {
    fun export(destination: Uri, playlist: UserPlaylist, songs: List<SongRef>) {
        val byUri = songs.associateBy(SongRef::uri)
        val body = buildString {
            appendLine("#EXTM3U")
            playlist.songUris.forEach { uri ->
                byUri[uri]?.let { song ->
                    appendLine("#EXTINF:${song.durationMs / 1000},${song.artist} - ${song.title}")
                    appendLine(uri)
                }
            }
        }
        context.contentResolver.openOutputStream(destination, "w")?.bufferedWriter()?.use { it.write(body) }
            ?: error("Could not write this playlist")
    }

    fun import(source: Uri, songs: List<SongRef>): Pair<String, List<String>> {
        val lines = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readLines() }
            ?: error("Could not read this playlist")
        val byUri = songs.associateBy(SongRef::uri)
        val byFile = songs.groupBy { it.fileName.lowercase() }
        val matches = lines.asSequence()
            .map(String::trim)
            .filter { it.isNotBlank() && !it.startsWith('#') }
            .mapNotNull { entry ->
                byUri[entry]?.uri ?: byFile[Uri.parse(entry).lastPathSegment?.lowercase()]?.singleOrNull()?.uri
            }
            .distinct()
            .toList()
        val name = source.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Imported playlist" }
            ?: "Imported playlist"
        return name to matches
    }
}
