package dev.swipetune.app

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.LruCache
import androidx.documentfile.provider.DocumentFile

class MusicRepository(private val context: Context) {
    private val indexer = LibraryIndexer(context)
    private val audioExtensions = setOf("mp3", "flac", "m4a", "aac", "wav", "ogg", "opus")
    private val artworkCache = object : LruCache<String, android.graphics.Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
    }

    fun scan(treeUri: Uri): List<SongRef> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("This music folder is no longer available. Choose it again in Settings.")
        if (!root.canRead()) error("SwipeTune no longer has permission to read this folder. Choose it again in Settings.")
        val result = mutableListOf<SongRef>()
        val pending = ArrayDeque<Pair<DocumentFile, String>>()
        pending.add(root to (root.name ?: "Music"))
        while (pending.isNotEmpty()) {
            val (item, folderName) = pending.removeFirst()
            item.listFiles().forEach { child ->
                when {
                    child.isDirectory -> pending.add(child to (child.name ?: folderName))
                    child.isFile && child.isAudio() -> result += readSong(child, folderName)
                }
            }
        }
        return result.distinctBy(SongRef::uri).sortedBy { it.title.lowercase() }
    }

    suspend fun scanAndIndex(treeUri: Uri): List<SongRef> {
        val started = System.currentTimeMillis()
        val safSongs = scan(treeUri)
        indexer.replaceSafRoot(treeUri.toString(), treeUri.lastPathSegment ?: "Music", safSongs, started)
        val mediaStoreSongs = indexer.indexMediaStoreIfPermitted()
        return (safSongs + mediaStoreSongs).distinctBy(SongRef::uri).sortedBy { it.title.lowercase() }
    }

    fun metadata(song: SongRef): TrackMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(song.uri))
            TrackMetadata(song.title, song.artist, song.album, decodeArtwork(song.uri, retriever.embeddedPicture))
        } finally {
            retriever.release()
        }
    }

    fun artwork(song: SongRef): android.graphics.Bitmap? {
        artworkCache.get(song.uri)?.let { return it }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(song.uri))
            decodeArtwork(song.uri, retriever.embeddedPicture)
        } finally {
            retriever.release()
        }
    }

    fun delete(song: SongRef): Boolean = runCatching {
        DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(song.uri))
    }.getOrDefault(false)

    private fun DocumentFile.isAudio(): Boolean {
        if (type?.startsWith("audio/") == true) return true
        return name?.substringAfterLast('.', "")?.lowercase() in audioExtensions
    }

    private fun readSong(file: DocumentFile, folderName: String): SongRef {
        val fileName = file.name ?: "Unknown track"
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, file.uri)
            SongRef(
                uri = file.uri.toString(),
                fileName = fileName,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf(String::isNotBlank) ?: fileName.substringBeforeLast('.'),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf(String::isNotBlank) ?: "Unknown artist",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf(String::isNotBlank) ?: "Unknown album",
                folderName = folderName,
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                    ?.takeIf(String::isNotBlank) ?: "Uncategorized",
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore('/')?.toIntOrNull() ?: 0,
            )
        }.getOrElse {
            SongRef(uri = file.uri.toString(), fileName = fileName, folderName = folderName)
        }.also { retriever.release() }
    }

    private fun decodeArtwork(uri: String, bytes: ByteArray?): android.graphics.Bitmap? {
        artworkCache.get(uri)?.let { return it }
        bytes ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 768 || bounds.outHeight / sample > 768) sample *= 2
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
        bitmap?.let { artworkCache.put(uri, it) }
        return bitmap
    }
}

