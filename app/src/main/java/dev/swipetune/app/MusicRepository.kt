package dev.swipetune.app

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

class MusicRepository(private val context: Context) {
    private val audioExtensions = setOf("mp3", "flac", "m4a", "aac", "wav", "ogg", "opus")

    fun scan(treeUri: Uri): List<SongRef> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val result = mutableListOf<SongRef>()
        val pending = ArrayDeque<DocumentFile>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()
            item.listFiles().forEach { child ->
                when {
                    child.isDirectory -> pending.add(child)
                    child.isFile && child.isAudio() -> result += SongRef(
                        uri = child.uri.toString(),
                        fileName = child.name ?: "Unknown track",
                    )
                }
            }
        }
        return result.distinctBy(SongRef::uri).sortedBy { it.fileName.lowercase() }
    }

    fun metadata(song: SongRef): TrackMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(song.uri))
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf(String::isNotBlank) ?: song.fileName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf(String::isNotBlank) ?: "Unknown artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf(String::isNotBlank) ?: "Unknown album"
            val art = retriever.embeddedPicture?.let { bytes ->
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            }
            TrackMetadata(title, artist, album, art)
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
}

