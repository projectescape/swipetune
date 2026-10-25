package dev.swipetune.app

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "tracks",
    indices = [
        Index("titleSort"), Index("albumKey"), Index("folderPath"), Index("dateModifiedMs"),
    ],
)
data class TrackEntity(
    @PrimaryKey val uri: String,
    val source: String,
    val fileName: String,
    val title: String,
    val titleSort: String,
    val artistDisplay: String,
    val albumTitle: String,
    val albumKey: String,
    val folderName: String,
    val folderPath: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int = 0,
    val year: Int = 0,
    val genreDisplay: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val replayGainTrackDb: Double? = null,
    val replayGainAlbumDb: Double? = null,
    val sizeBytes: Long = 0,
    val dateModifiedMs: Long = 0,
    val artworkKey: String? = null,
    val mediaStoreId: Long? = null,
    val lastIndexedMs: Long,
    val missing: Boolean = false,
)

@Entity(tableName = "artists")
data class ArtistEntity(@PrimaryKey val key: String, val name: String, val sortName: String)

@Entity(tableName = "albums", indices = [Index("titleSort")])
data class AlbumEntity(
    @PrimaryKey val key: String,
    val title: String,
    val titleSort: String,
    val albumArtist: String,
    val year: Int = 0,
    val artworkKey: String? = null,
)

@Entity(tableName = "genres")
data class GenreEntity(@PrimaryKey val key: String, val name: String, val sortName: String)

@Entity(
    tableName = "track_artists",
    primaryKeys = ["trackUri", "artistKey"],
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["key"], childColumns = ["artistKey"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("artistKey")],
)
data class TrackArtistCrossRef(val trackUri: String, val artistKey: String, val position: Int)

@Entity(
    tableName = "track_genres",
    primaryKeys = ["trackUri", "genreKey"],
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GenreEntity::class, parentColumns = ["key"], childColumns = ["genreKey"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("genreKey")],
)
data class TrackGenreCrossRef(val trackUri: String, val genreKey: String)

@Entity(tableName = "scan_roots")
data class ScanRootEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val source: String,
    val enabled: Boolean = true,
    val lastScanMs: Long = 0,
    val generation: Long = 0,
)

@Entity(tableName = "folder_exclusions")
data class FolderExclusionEntity(@PrimaryKey val uriOrPath: String)

@Entity(tableName = "favorites", foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE)])
data class FavoriteEntity(@PrimaryKey val trackUri: String, val addedAtMs: Long)

@Entity(tableName = "review_decisions", foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE)])
data class ReviewDecisionEntity(@PrimaryKey val trackUri: String, val decision: String, val decidedAtMs: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val coverUri: String? = null,
    val smartRuleJson: String? = null,
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "trackUri"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackEntity::class, parentColumns = ["uri"], childColumns = ["trackUri"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("trackUri"), Index(value = ["playlistId", "position"], unique = true)],
)
data class PlaylistItemEntity(val playlistId: String, val trackUri: String, val position: Int, val addedAtMs: Long)

@Entity(tableName = "play_events", indices = [Index("trackUri"), Index("startedAtMs")])
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackUri: String,
    val startedAtMs: Long,
    val listenedMs: Long,
    val completed: Boolean,
    val skipped: Boolean,
    val source: String,
)

@Entity(tableName = "app_markers")
data class AppMarkerEntity(@PrimaryKey val key: String, val value: String)

data class AlbumSummary(
    val albumKey: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val durationMs: Long,
)

data class ArtistSummary(val artist: String, val trackCount: Int, val albumCount: Int)
data class GenreSummary(val genre: String, val trackCount: Int)
data class FolderSummary(val folderPath: String, val folderName: String, val trackCount: Int)
data class HistorySummary(val trackUri: String, val lastPlayedMs: Long, val playCount: Int, val listenedMs: Long)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY titleSort, uri")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY titleSort, uri")
    suspend fun tracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE uri = :uri AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') LIMIT 1")
    suspend fun track(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE albumKey = :albumKey AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY discNumber, trackNumber, titleSort")
    fun observeAlbumTracks(albumKey: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE albumKey = :albumKey AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY discNumber, trackNumber, titleSort")
    suspend fun albumTracks(albumKey: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE artistDisplay = :artist AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY albumTitle COLLATE NOCASE, discNumber, trackNumber, titleSort")
    suspend fun artistTracks(artist: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE genreDisplay = :genre AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY artistDisplay COLLATE NOCASE, albumTitle COLLATE NOCASE, trackNumber")
    suspend fun genreTracks(genre: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE folderPath = :folderPath AND missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') ORDER BY titleSort")
    suspend fun folderTracks(folderPath: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') AND (title LIKE '%' || :query || '%' OR artistDisplay LIKE '%' || :query || '%' OR albumTitle LIKE '%' || :query || '%') ORDER BY titleSort LIMIT :limit")
    suspend fun searchTracks(query: String, limit: Int = 200): List<TrackEntity>

    @Query("SELECT albumKey, albumTitle AS title, artistDisplay AS artist, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY albumKey ORDER BY title COLLATE NOCASE")
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query("SELECT albumKey, albumTitle AS title, artistDisplay AS artist, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY albumKey ORDER BY title COLLATE NOCASE")
    suspend fun albumsOnce(): List<AlbumSummary>

    @Query("SELECT artistDisplay AS artist, COUNT(*) AS trackCount, COUNT(DISTINCT albumKey) AS albumCount FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY artistDisplay ORDER BY artist COLLATE NOCASE")
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query("SELECT artistDisplay AS artist, COUNT(*) AS trackCount, COUNT(DISTINCT albumKey) AS albumCount FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY artistDisplay ORDER BY artist COLLATE NOCASE")
    suspend fun artistsOnce(): List<ArtistSummary>

    @Query("SELECT genreDisplay AS genre, COUNT(*) AS trackCount FROM tracks WHERE missing = 0 AND genreDisplay != '' AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY genreDisplay ORDER BY genre COLLATE NOCASE")
    fun observeGenres(): Flow<List<GenreSummary>>

    @Query("SELECT genreDisplay AS genre, COUNT(*) AS trackCount FROM tracks WHERE missing = 0 AND genreDisplay != '' AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY genreDisplay ORDER BY genre COLLATE NOCASE")
    suspend fun genresOnce(): List<GenreSummary>

    @Query("SELECT folderPath, folderName, COUNT(*) AS trackCount FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY folderPath, folderName ORDER BY folderPath COLLATE NOCASE")
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query("SELECT folderPath, folderName, COUNT(*) AS trackCount FROM tracks WHERE missing = 0 AND uri NOT IN (SELECT trackUri FROM review_decisions WHERE decision = 'REJECTED') GROUP BY folderPath, folderName ORDER BY folderPath COLLATE NOCASE")
    suspend fun foldersOnce(): List<FolderSummary>

    @Upsert suspend fun upsertTracks(items: List<TrackEntity>)
    @Upsert suspend fun upsertArtists(items: List<ArtistEntity>)
    @Upsert suspend fun upsertAlbums(items: List<AlbumEntity>)
    @Upsert suspend fun upsertGenres(items: List<GenreEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTrackArtists(items: List<TrackArtistCrossRef>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTrackGenres(items: List<TrackGenreCrossRef>)
    @Upsert suspend fun putRoot(root: ScanRootEntity)

    @Query("UPDATE tracks SET missing = 1 WHERE source = :source AND lastIndexedMs < :scanStartedMs")
    suspend fun markStaleMissing(source: String, scanStartedMs: Long)

    @Query("DELETE FROM track_artists WHERE trackUri IN (:uris)")
    suspend fun clearTrackArtists(uris: List<String>)

    @Query("DELETE FROM track_genres WHERE trackUri IN (:uris)")
    suspend fun clearTrackGenres(uris: List<String>)

    @Upsert suspend fun putFavorites(items: List<FavoriteEntity>)
    @Upsert suspend fun putReviewDecisions(items: List<ReviewDecisionEntity>)
    @Query("DELETE FROM review_decisions") suspend fun clearReviewDecisions()
    @Upsert suspend fun putPlaylists(items: List<PlaylistEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPlaylistItems(items: List<PlaylistItemEntity>)
    @Insert suspend fun addPlayEvent(event: PlayEventEntity): Long

    @Query("SELECT * FROM play_events ORDER BY startedAtMs")
    suspend fun playEventsOnce(): List<PlayEventEntity>

    @Query("DELETE FROM play_events")
    suspend fun clearPlayEvents()

    @Insert suspend fun addPlayEvents(events: List<PlayEventEntity>)

    @Query("SELECT trackUri, MAX(startedAtMs) AS lastPlayedMs, COUNT(*) AS playCount, SUM(listenedMs) AS listenedMs FROM play_events GROUP BY trackUri ORDER BY lastPlayedMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<HistorySummary>>

    @Query("SELECT * FROM app_markers WHERE `key` = :key LIMIT 1")
    suspend fun marker(key: String): AppMarkerEntity?
    @Upsert suspend fun putMarker(marker: AppMarkerEntity)
}

@Database(
    entities = [
        TrackEntity::class, ArtistEntity::class, AlbumEntity::class, GenreEntity::class,
        TrackArtistCrossRef::class, TrackGenreCrossRef::class, ScanRootEntity::class,
        FolderExclusionEntity::class, FavoriteEntity::class, ReviewDecisionEntity::class,
        PlaylistEntity::class, PlaylistItemEntity::class, PlayEventEntity::class, AppMarkerEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SwipeTuneDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile private var instance: SwipeTuneDatabase? = null

        fun get(context: Context): SwipeTuneDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SwipeTuneDatabase::class.java, "swipetune.db")
                .enableMultiInstanceInvalidation()
                .build()
                .also { instance = it }
        }
    }
}

class LegacyStateMigrator(private val database: SwipeTuneDatabase) {
    suspend fun migrate(state: PersistedState) {
        if (database.libraryDao().marker(MARKER) != null) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            val dao = database.libraryDao()
            val tracks = state.songs.map { it.toEntity(source = "legacy", indexedAt = now) }
            dao.upsertTracks(tracks)
            if (state.favorites.isNotEmpty()) dao.putFavorites(state.favorites.map { FavoriteEntity(it, now) })
            if (state.decisions.isNotEmpty()) dao.putReviewDecisions(
                state.decisions.map { (uri, decision) -> ReviewDecisionEntity(uri, decision.name, now) },
            )
            if (state.playlists.isNotEmpty()) {
                dao.putPlaylists(state.playlists.map { PlaylistEntity(it.id, it.name, now, now, coverUri = it.coverUri) })
                dao.putPlaylistItems(state.playlists.flatMap { playlist ->
                    playlist.songUris.mapIndexed { index, uri -> PlaylistItemEntity(playlist.id, uri, index, now) }
                })
            }
            dao.putMarker(AppMarkerEntity(MARKER, "1"))
        }
    }

    private companion object { const val MARKER = "legacy_json_import_v1" }
}

internal fun SongRef.toEntity(source: String, indexedAt: Long): TrackEntity = TrackEntity(
    uri = uri,
    source = source,
    fileName = fileName,
    title = title,
    titleSort = title.lowercase(),
    artistDisplay = artist,
    albumTitle = album,
    albumKey = "${album.lowercase()}|${artist.lowercase()}",
    folderName = folderName,
    folderPath = folderName,
    genreDisplay = genre,
    durationMs = durationMs,
    trackNumber = trackNumber,
    lastIndexedMs = indexedAt,
)

internal fun TrackEntity.toSongRef(): SongRef = SongRef(
    uri = uri,
    fileName = fileName,
    title = title,
    artist = artistDisplay,
    album = albumTitle,
    folderName = folderName,
    genre = genreDisplay.ifBlank { "Uncategorized" },
    durationMs = durationMs,
    trackNumber = trackNumber,
)
