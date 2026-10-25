package dev.swipetune.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.room.withTransaction

class LibraryIndexer(
    private val context: Context,
    private val database: SwipeTuneDatabase = SwipeTuneDatabase.get(context),
) {
    suspend fun replaceSafRoot(rootUri: String, displayName: String, songs: List<SongRef>, scanStartedMs: Long) {
        index("saf:$rootUri", songs, scanStartedMs)
        database.libraryDao().putRoot(
            ScanRootEntity(rootUri, displayName, "saf", lastScanMs = System.currentTimeMillis()),
        )
    }

    suspend fun indexMediaStoreIfPermitted(): List<SongRef> {
        if (!hasMediaPermission()) return emptyList()
        val started = System.currentTimeMillis()
        val songs = queryMediaStore(started)
        index(MEDIA_STORE_SOURCE, songs.map(IndexedTrack::song), started, songs)
        database.libraryDao().putRoot(
            ScanRootEntity(MEDIA_STORE_ROOT, "Device audio", "media_store", lastScanMs = System.currentTimeMillis()),
        )
        return songs.map(IndexedTrack::song)
    }

    private suspend fun index(
        source: String,
        songs: List<SongRef>,
        scanStartedMs: Long,
        enriched: List<IndexedTrack> = emptyList(),
    ) {
        val enrichedByUri = enriched.associateBy { it.song.uri }
        database.withTransaction {
            val dao = database.libraryDao()
            val tracks = songs.map { song ->
                val detail = enrichedByUri[song.uri]
                song.toEntity(source, scanStartedMs).copy(
                    folderPath = detail?.folderPath ?: song.folderName,
                    sizeBytes = detail?.sizeBytes ?: 0,
                    dateModifiedMs = detail?.dateModifiedMs ?: 0,
                    mediaStoreId = detail?.mediaStoreId,
                    albumKey = albumKey(song.album, song.artist),
                    artworkKey = detail?.mediaStoreId?.let { "mediastore:$it" },
                )
            }
            if (tracks.isNotEmpty()) dao.upsertTracks(tracks)
            val artists = songs.flatMap { splitArtists(it.artist) }.distinctBy(::normalKey)
                .map { ArtistEntity(normalKey(it), it, it.lowercase()) }
            val albums = songs.distinctBy { albumKey(it.album, it.artist) }.map {
                AlbumEntity(albumKey(it.album, it.artist), it.album, it.album.lowercase(), it.artist)
            }
            if (artists.isNotEmpty()) dao.upsertArtists(artists)
            if (albums.isNotEmpty()) dao.upsertAlbums(albums)
            val uris = songs.map(SongRef::uri)
            if (uris.isNotEmpty()) {
                dao.clearTrackArtists(uris)
                dao.putTrackArtists(songs.flatMap { song ->
                    splitArtists(song.artist).mapIndexed { position, artist ->
                        TrackArtistCrossRef(song.uri, normalKey(artist), position)
                    }
                })
            }
            dao.markStaleMissing(source, scanStartedMs)
        }
    }

    private fun queryMediaStore(indexedAt: Long): List<IndexedTrack> {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        return buildList {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val fileIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val path = cursor.getString(pathIndex).orEmpty().trimEnd('/')
                    val fileName = cursor.getString(fileIndex) ?: "Unknown track"
                    val song = SongRef(
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        fileName = fileName,
                        title = cursor.getString(titleIndex)?.takeIf(String::isNotBlank) ?: fileName.substringBeforeLast('.'),
                        artist = cursor.getString(artistIndex)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist",
                        album = cursor.getString(albumIndex)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown album",
                        folderName = path.substringAfterLast('/').ifBlank { "Music" },
                        durationMs = cursor.getLong(durationIndex),
                        trackNumber = cursor.getInt(trackIndex) % 1000,
                    )
                    add(
                        IndexedTrack(
                            song = song,
                            folderPath = path,
                            sizeBytes = cursor.getLong(sizeIndex),
                            dateModifiedMs = cursor.getLong(modifiedIndex) * 1000,
                            mediaStoreId = id,
                            indexedAt = indexedAt,
                        ),
                    )
                }
            }
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun splitArtists(display: String): List<String> = display
        .split(Regex("\\s*(?:;|,|&| feat\\.? | featuring )\\s*", RegexOption.IGNORE_CASE))
        .map(String::trim)
        .filter(String::isNotBlank)
        .ifEmpty { listOf("Unknown artist") }

    private fun normalKey(value: String): String = value.trim().lowercase()
    private fun albumKey(album: String, artist: String): String = "${normalKey(album)}|${normalKey(artist)}"

    private data class IndexedTrack(
        val song: SongRef,
        val folderPath: String,
        val sizeBytes: Long,
        val dateModifiedMs: Long,
        val mediaStoreId: Long,
        val indexedAt: Long,
    )

    private companion object {
        const val MEDIA_STORE_ROOT = "content://media/external/audio/media"
        const val MEDIA_STORE_SOURCE = "media_store"
    }
}
