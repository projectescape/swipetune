package dev.swipetune.app

data class DuplicateGroup(val key: String, val songs: List<SongRef>, val recommendedKeepUri: String)

object DuplicateFinder {
    fun find(songs: List<SongRef>): List<DuplicateGroup> = songs
        .groupBy { song ->
            val durationBucket = if (song.durationMs > 0) song.durationMs / 2_000 else -1
            "${normalize(song.title)}|${normalize(song.artist)}|$durationBucket"
        }
        .filterValues { it.size > 1 }
        .map { (key, matches) ->
            val ranked = matches.sortedWith(
                compareByDescending<SongRef> { qualityRank(it.fileName) }
                    .thenByDescending { it.durationMs }
                    .thenBy { it.fileName.length },
            )
            DuplicateGroup(key, ranked, ranked.first().uri)
        }
        .sortedByDescending { it.songs.size }

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("\\([^)]*(?:remaster|version|edit)[^)]*\\)"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun qualityRank(fileName: String): Int = when (fileName.substringAfterLast('.', "").lowercase()) {
        "flac", "wav" -> 5
        "m4a", "aac" -> 4
        "opus", "ogg" -> 3
        "mp3" -> 2
        else -> 1
    }
}
