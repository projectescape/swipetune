package dev.swipetune.app

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

data class LyricLine(val timestampMs: Long?, val text: String)

class LyricsRepository(private val context: Context) {
    private val directory = context.filesDir.resolve("lyrics").apply { mkdirs() }

    fun load(trackUri: String): List<LyricLine> {
        val file = directory.resolve("${key(trackUri)}.lrc")
        return if (file.exists()) parse(file.readText()) else emptyList()
    }

    fun import(trackUri: String, source: Uri): List<LyricLine> {
        val text = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read this lyrics file")
        val parsed = parse(text)
        require(parsed.isNotEmpty()) { "This lyrics file is empty" }
        directory.resolve("${key(trackUri)}.lrc").writeText(text)
        return parsed
    }

    fun parse(text: String): List<LyricLine> = text.lineSequence().mapNotNull { raw ->
        val line = raw.trim()
        if (line.isBlank()) return@mapNotNull null
        val match = TIMESTAMP.find(line)
        if (match == null) LyricLine(null, line) else {
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            LyricLine((minutes * 60 + seconds) * 1000 + fraction, line.removeRange(match.range).trim())
        }
    }.filter { it.text.isNotBlank() }.sortedWith(compareBy(nullsLast()) { it.timestampMs }).toList()

    private fun key(uri: String): String = MessageDigest.getInstance("SHA-256")
        .digest(uri.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        val TIMESTAMP = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    }
}
