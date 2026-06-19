package cn.xuexc.ai_player.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MusicDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "music_player.db"
        private const val DATABASE_VERSION = 5

        const val TABLE_SONGS = "songs"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ARTIST = "artist"
        const val COLUMN_ALBUM = "album"
        const val COLUMN_PATH = "path"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_SIZE = "size"
        const val COLUMN_ALBUM_ID = "album_id"
        const val COLUMN_IS_FAVORITE = "is_favorite"
        const val COLUMN_IS_BLACKLISTED = "is_blacklisted"
        const val COLUMN_DATE_ADDED = "date_added"
        const val COLUMN_FAVORITED_AT = "favorited_at"
        const val COLUMN_BLACKLISTED_AT = "blacklisted_at"
        const val COLUMN_LAST_MODIFIED = "last_modified"

        // Playlists table
        const val TABLE_PLAYLISTS = "playlists"
        const val COLUMN_PLAYLIST_ID = "id"
        const val COLUMN_PLAYLIST_NAME = "name"

        // Playlist songs mapping table
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        const val COLUMN_PS_PLAYLIST_ID = "playlist_id"
        const val COLUMN_PS_SONG_ID = "song_id"
        const val COLUMN_PS_ADDED_AT = "added_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSongsTable =
            """
            CREATE TABLE $TABLE_SONGS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_TITLE TEXT,
                $COLUMN_ARTIST TEXT,
                $COLUMN_ALBUM TEXT,
                $COLUMN_PATH TEXT,
                $COLUMN_DURATION INTEGER,
                $COLUMN_SIZE INTEGER,
                $COLUMN_ALBUM_ID INTEGER,
                $COLUMN_IS_FAVORITE INTEGER DEFAULT 0,
                $COLUMN_IS_BLACKLISTED INTEGER DEFAULT 0,
                $COLUMN_DATE_ADDED INTEGER DEFAULT 0,
                $COLUMN_FAVORITED_AT INTEGER DEFAULT 0,
                $COLUMN_BLACKLISTED_AT INTEGER DEFAULT 0,
                $COLUMN_LAST_MODIFIED INTEGER DEFAULT 0
            )
            """
                .trimIndent()
        db.execSQL(createSongsTable)

        val createPlaylistsTable =
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                $COLUMN_PLAYLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYLIST_NAME TEXT UNIQUE
            )
            """
                .trimIndent()
        db.execSQL(createPlaylistsTable)

        val createPlaylistSongsTable =
            """
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                $COLUMN_PS_PLAYLIST_ID INTEGER,
                $COLUMN_PS_SONG_ID INTEGER,
                $COLUMN_PS_ADDED_AT INTEGER DEFAULT 0,
                PRIMARY KEY ($COLUMN_PS_PLAYLIST_ID, $COLUMN_PS_SONG_ID),
                FOREIGN KEY ($COLUMN_PS_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS ($COLUMN_PLAYLIST_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_PS_SONG_ID) REFERENCES $TABLE_SONGS ($COLUMN_ID) ON DELETE CASCADE
            )
            """
                .trimIndent()
        db.execSQL(createPlaylistSongsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createPlaylistsTable =
                """
                CREATE TABLE $TABLE_PLAYLISTS (
                    $COLUMN_PLAYLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_PLAYLIST_NAME TEXT UNIQUE
                )
                """
                    .trimIndent()
            db.execSQL(createPlaylistsTable)

            val createPlaylistSongsTable =
                """
                CREATE TABLE $TABLE_PLAYLIST_SONGS (
                    $COLUMN_PS_PLAYLIST_ID INTEGER,
                    $COLUMN_PS_SONG_ID INTEGER,
                    $COLUMN_PS_ADDED_AT INTEGER DEFAULT 0,
                    PRIMARY KEY ($COLUMN_PS_PLAYLIST_ID, $COLUMN_PS_SONG_ID),
                    FOREIGN KEY ($COLUMN_PS_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS ($COLUMN_PLAYLIST_ID) ON DELETE CASCADE,
                    FOREIGN KEY ($COLUMN_PS_SONG_ID) REFERENCES $TABLE_SONGS ($COLUMN_ID) ON DELETE CASCADE
                )
                """
                    .trimIndent()
            db.execSQL(createPlaylistSongsTable)
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN $COLUMN_DATE_ADDED INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN $COLUMN_FAVORITED_AT INTEGER DEFAULT 0")
            db.execSQL(
                "ALTER TABLE $TABLE_SONGS ADD COLUMN $COLUMN_BLACKLISTED_AT INTEGER DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_PLAYLIST_SONGS ADD COLUMN $COLUMN_PS_ADDED_AT INTEGER DEFAULT 0"
            )
        }
        if (oldVersion < 5) {
            db.execSQL(
                "ALTER TABLE $TABLE_SONGS ADD COLUMN $COLUMN_LAST_MODIFIED INTEGER DEFAULT 0"
            )
        }
    }

    fun syncSongs(scannedSongs: List<Song>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existingSongs = getAllSongsInternal(db, includeBlacklisted = true)
            val existingMap = existingSongs.associateBy { it.id }
            val scannedIds = scannedSongs.map { it.id }.toSet()

            // 1. Insert or update scanned songs
            for (song in scannedSongs) {
                val cv =
                    ContentValues().apply {
                        put(COLUMN_ID, song.id)
                        put(COLUMN_TITLE, song.title)
                        put(COLUMN_ARTIST, song.artist)
                        put(COLUMN_ALBUM, song.album)
                        put(COLUMN_PATH, song.path)
                        put(COLUMN_DURATION, song.duration)
                        put(COLUMN_SIZE, song.size)
                        put(COLUMN_ALBUM_ID, song.albumId)
                        put(COLUMN_DATE_ADDED, song.dateAdded)
                        val file = java.io.File(song.path)
                        val lastMod = if (file.exists()) file.lastModified() else 0L
                        put(COLUMN_LAST_MODIFIED, lastMod)
                    }

                if (existingMap.containsKey(song.id)) {
                    val existing = existingMap[song.id]
                    val file = java.io.File(song.path)
                    val currentLastModified = if (file.exists()) file.lastModified() else 0L
                    if (
                        existing != null &&
                            (existing.title != song.title ||
                                existing.artist != song.artist ||
                                existing.album != song.album ||
                                existing.duration != song.duration ||
                                existing.size != song.size ||
                                existing.lastModified != currentLastModified)
                    ) {
                        clearCoverCacheForSong(context, song.id)
                    }
                    // Update but preserve flags (which are not in ContentValues)
                    db.update(TABLE_SONGS, cv, "$COLUMN_ID = ?", arrayOf(song.id.toString()))
                } else {
                    // New song, set defaults
                    cv.put(COLUMN_IS_FAVORITE, 0)
                    cv.put(COLUMN_IS_BLACKLISTED, 0)
                    db.insert(TABLE_SONGS, null, cv)
                }
            }

            // 2. Delete songs no longer present on device
            for (existingSong in existingSongs) {
                if (!scannedIds.contains(existingSong.id)) {
                    db.delete(TABLE_SONGS, "$COLUMN_ID = ?", arrayOf(existingSong.id.toString()))
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllSongs(
        includeBlacklisted: Boolean = false,
        orderBy: String = "$COLUMN_TITLE ASC",
    ): List<Song> {
        return getAllSongsInternal(readableDatabase, includeBlacklisted, orderBy)
    }

    private fun getAllSongsInternal(
        db: SQLiteDatabase,
        includeBlacklisted: Boolean,
        orderBy: String = "$COLUMN_TITLE ASC",
    ): List<Song> {
        val songsList = mutableListOf<Song>()
        val selection = if (includeBlacklisted) null else "$COLUMN_IS_BLACKLISTED = 0"
        val cursor = db.query(TABLE_SONGS, null, selection, null, null, null, orderBy)

        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val titleCol = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(COLUMN_ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM)
            val pathCol = cursor.getColumnIndexOrThrow(COLUMN_PATH)
            val durationCol = cursor.getColumnIndexOrThrow(COLUMN_DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(COLUMN_SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID)
            val favCol = cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
            val blackCol = cursor.getColumnIndexOrThrow(COLUMN_IS_BLACKLISTED)
            val dateAddedCol = cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)
            val lastModCol = cursor.getColumnIndex(COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                songsList.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        path = cursor.getString(pathCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        albumId = cursor.getLong(albumIdCol),
                        isFavorite = cursor.getInt(favCol) == 1,
                        isBlacklisted = cursor.getInt(blackCol) == 1,
                        dateAdded = cursor.getLong(dateAddedCol),
                        lastModified = if (lastModCol != -1) cursor.getLong(lastModCol) else 0L,
                    )
                )
            }
        }
        return songsList
    }

    fun getFavoriteSongs(): List<Song> {
        val db = readableDatabase
        val songsList = mutableListOf<Song>()
        val cursor =
            db.query(
                TABLE_SONGS,
                null,
                "$COLUMN_IS_FAVORITE = 1 AND $COLUMN_IS_BLACKLISTED = 0",
                null,
                null,
                null,
                "$COLUMN_FAVORITED_AT DESC, $COLUMN_ID DESC",
            )

        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val titleCol = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(COLUMN_ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM)
            val pathCol = cursor.getColumnIndexOrThrow(COLUMN_PATH)
            val durationCol = cursor.getColumnIndexOrThrow(COLUMN_DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(COLUMN_SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID)
            val favCol = cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
            val blackCol = cursor.getColumnIndexOrThrow(COLUMN_IS_BLACKLISTED)
            val dateAddedCol = cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)
            val lastModCol = cursor.getColumnIndex(COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                songsList.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        path = cursor.getString(pathCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        albumId = cursor.getLong(albumIdCol),
                        isFavorite = cursor.getInt(favCol) == 1,
                        isBlacklisted = cursor.getInt(blackCol) == 1,
                        dateAdded = cursor.getLong(dateAddedCol),
                        lastModified = if (lastModCol != -1) cursor.getLong(lastModCol) else 0L,
                    )
                )
            }
        }
        return songsList
    }

    fun getFavoriteSongsCount(): Int {
        val db = readableDatabase
        val cursor =
            db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_SONGS WHERE $COLUMN_IS_FAVORITE = 1 AND $COLUMN_IS_BLACKLISTED = 0",
                null,
            )
        cursor.use {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    fun getBlacklistedSongs(): List<Song> {
        val db = readableDatabase
        val songsList = mutableListOf<Song>()
        val cursor =
            db.query(
                TABLE_SONGS,
                null,
                "$COLUMN_IS_BLACKLISTED = 1",
                null,
                null,
                null,
                "$COLUMN_BLACKLISTED_AT DESC, $COLUMN_ID DESC",
            )

        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val titleCol = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(COLUMN_ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM)
            val pathCol = cursor.getColumnIndexOrThrow(COLUMN_PATH)
            val durationCol = cursor.getColumnIndexOrThrow(COLUMN_DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(COLUMN_SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID)
            val favCol = cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
            val blackCol = cursor.getColumnIndexOrThrow(COLUMN_IS_BLACKLISTED)
            val dateAddedCol = cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)
            val lastModCol = cursor.getColumnIndex(COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                songsList.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        path = cursor.getString(pathCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        albumId = cursor.getLong(albumIdCol),
                        isFavorite = cursor.getInt(favCol) == 1,
                        isBlacklisted = cursor.getInt(blackCol) == 1,
                        dateAdded = cursor.getLong(dateAddedCol),
                        lastModified = if (lastModCol != -1) cursor.getLong(lastModCol) else 0L,
                    )
                )
            }
        }
        return songsList
    }

    fun setFavorite(songId: Long, isFavorite: Boolean) {
        val db = writableDatabase
        val cv =
            ContentValues().apply {
                put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
                if (isFavorite) {
                    put(COLUMN_FAVORITED_AT, System.currentTimeMillis() / 1000)
                }
            }
        db.update(TABLE_SONGS, cv, "$COLUMN_ID = ?", arrayOf(songId.toString()))
    }

    fun setBlacklisted(songId: Long, isBlacklisted: Boolean) {
        val db = writableDatabase
        val cv =
            ContentValues().apply {
                put(COLUMN_IS_BLACKLISTED, if (isBlacklisted) 1 else 0)
                if (isBlacklisted) {
                    put(COLUMN_BLACKLISTED_AT, System.currentTimeMillis() / 1000)
                }
            }
        db.update(TABLE_SONGS, cv, "$COLUMN_ID = ?", arrayOf(songId.toString()))
    }

    // --- Playlist Methods ---

    fun createPlaylist(name: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COLUMN_PLAYLIST_NAME, name) }
        return db.insertWithOnConflict(TABLE_PLAYLISTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getPlaylistIdByName(name: String): Long? {
        val db = readableDatabase
        val cursor =
            db.query(
                TABLE_PLAYLISTS,
                arrayOf(COLUMN_PLAYLIST_ID),
                "$COLUMN_PLAYLIST_NAME = ?",
                arrayOf(name),
                null,
                null,
                null,
            )
        cursor.use {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    fun insertSong(song: Song): Long {
        val db = writableDatabase
        val cv =
            ContentValues().apply {
                put(COLUMN_ID, song.id)
                put(COLUMN_TITLE, song.title)
                put(COLUMN_ARTIST, song.artist)
                put(COLUMN_ALBUM, song.album)
                put(COLUMN_PATH, song.path)
                put(COLUMN_DURATION, song.duration)
                put(COLUMN_SIZE, song.size)
                put(COLUMN_ALBUM_ID, song.albumId)
                put(COLUMN_IS_FAVORITE, if (song.isFavorite) 1 else 0)
                put(COLUMN_IS_BLACKLISTED, if (song.isBlacklisted) 1 else 0)
                put(COLUMN_DATE_ADDED, System.currentTimeMillis() / 1000)
            }
        return db.insertWithOnConflict(TABLE_SONGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun deleteSong(songId: Long) {
        val db = writableDatabase
        db.delete(TABLE_SONGS, "$COLUMN_ID = ?", arrayOf(songId.toString()))
        db.delete(TABLE_PLAYLIST_SONGS, "$COLUMN_PS_SONG_ID = ?", arrayOf(songId.toString()))
    }

    fun deletePlaylist(playlistId: Long) {
        val db = writableDatabase
        db.delete(TABLE_PLAYLISTS, "$COLUMN_PLAYLIST_ID = ?", arrayOf(playlistId.toString()))
        db.delete(
            TABLE_PLAYLIST_SONGS,
            "$COLUMN_PS_PLAYLIST_ID = ?",
            arrayOf(playlistId.toString()),
        )
    }

    fun getPlaylists(): List<Playlist> {
        val db = readableDatabase
        val playlists = mutableListOf<Playlist>()
        val query =
            """
            SELECT p.$COLUMN_PLAYLIST_ID, p.$COLUMN_PLAYLIST_NAME, COUNT(s.$COLUMN_ID) as count,
                   (SELECT ps2.$COLUMN_PS_SONG_ID 
                    FROM $TABLE_PLAYLIST_SONGS ps2 
                    INNER JOIN $TABLE_SONGS s2 ON ps2.$COLUMN_PS_SONG_ID = s2.$COLUMN_ID 
                    WHERE ps2.$COLUMN_PS_PLAYLIST_ID = p.$COLUMN_PLAYLIST_ID AND s2.$COLUMN_IS_BLACKLISTED = 0 
                    LIMIT 1) as first_song_id
            FROM $TABLE_PLAYLISTS p
            LEFT JOIN $TABLE_PLAYLIST_SONGS ps ON p.$COLUMN_PLAYLIST_ID = ps.$COLUMN_PS_PLAYLIST_ID
            LEFT JOIN $TABLE_SONGS s ON ps.$COLUMN_PS_SONG_ID = s.$COLUMN_ID AND s.$COLUMN_IS_BLACKLISTED = 0
            GROUP BY p.$COLUMN_PLAYLIST_ID
            ORDER BY p.$COLUMN_PLAYLIST_NAME ASC
            """
                .trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_ID)
            val nameCol = cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_NAME)
            val countCol = cursor.getColumnIndexOrThrow("count")
            val firstSongIdCol = cursor.getColumnIndex("first_song_id")

            while (cursor.moveToNext()) {
                val firstSongId =
                    if (firstSongIdCol != -1 && !cursor.isNull(firstSongIdCol))
                        cursor.getLong(firstSongIdCol)
                    else null
                playlists.add(
                    Playlist(
                        id = cursor.getLong(idCol),
                        name = cursor.getString(nameCol) ?: "",
                        songCount = cursor.getInt(countCol),
                        firstSongId = firstSongId,
                    )
                )
            }
        }
        return playlists
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long): Boolean {
        val db = writableDatabase
        val cv =
            ContentValues().apply {
                put(COLUMN_PS_PLAYLIST_ID, playlistId)
                put(COLUMN_PS_SONG_ID, songId)
                put(COLUMN_PS_ADDED_AT, System.currentTimeMillis() / 1000)
            }
        val result =
            db.insertWithOnConflict(TABLE_PLAYLIST_SONGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        return result != -1L
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val db = writableDatabase
        db.delete(
            TABLE_PLAYLIST_SONGS,
            "$COLUMN_PS_PLAYLIST_ID = ? AND $COLUMN_PS_SONG_ID = ?",
            arrayOf(playlistId.toString(), songId.toString()),
        )
    }

    fun getSongsInPlaylist(playlistId: Long): List<Song> {
        val db = readableDatabase
        val songsList = mutableListOf<Song>()
        val query =
            """
            SELECT s.* 
            FROM $TABLE_SONGS s 
            INNER JOIN $TABLE_PLAYLIST_SONGS ps ON s.$COLUMN_ID = ps.$COLUMN_PS_SONG_ID 
            WHERE ps.$COLUMN_PS_PLAYLIST_ID = ? AND s.$COLUMN_IS_BLACKLISTED = 0
            ORDER BY ps.$COLUMN_PS_ADDED_AT DESC, s.$COLUMN_ID DESC
            """
                .trimIndent()

        val cursor = db.rawQuery(query, arrayOf(playlistId.toString()))
        cursor.use {
            val idCol = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val titleCol = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(COLUMN_ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM)
            val pathCol = cursor.getColumnIndexOrThrow(COLUMN_PATH)
            val durationCol = cursor.getColumnIndexOrThrow(COLUMN_DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(COLUMN_SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID)
            val favCol = cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
            val blackCol = cursor.getColumnIndexOrThrow(COLUMN_IS_BLACKLISTED)
            val dateAddedCol = cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)
            val lastModCol = cursor.getColumnIndex(COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                songsList.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        path = cursor.getString(pathCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        size = cursor.getLong(sizeCol),
                        albumId = cursor.getLong(albumIdCol),
                        isFavorite = cursor.getInt(favCol) == 1,
                        isBlacklisted = cursor.getInt(blackCol) == 1,
                        dateAdded = cursor.getLong(dateAddedCol),
                        lastModified = if (lastModCol != -1) cursor.getLong(lastModCol) else 0L,
                    )
                )
            }
        }
        return songsList
    }

    fun setFavoriteBatch(songIds: List<Long>, isFavorite: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv =
                ContentValues().apply {
                    put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
                    if (isFavorite) {
                        put(COLUMN_FAVORITED_AT, System.currentTimeMillis() / 1000)
                    }
                }
            for (id in songIds) {
                db.update(TABLE_SONGS, cv, "$COLUMN_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setBlacklistedBatch(songIds: List<Long>, isBlacklisted: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv =
                ContentValues().apply {
                    put(COLUMN_IS_BLACKLISTED, if (isBlacklisted) 1 else 0)
                    if (isBlacklisted) {
                        put(COLUMN_BLACKLISTED_AT, System.currentTimeMillis() / 1000)
                    }
                }
            for (id in songIds) {
                db.update(TABLE_SONGS, cv, "$COLUMN_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addSongsToPlaylistBatch(playlistId: Long, songIds: List<Long>): Int {
        val db = writableDatabase
        var addedCount = 0
        db.beginTransaction()
        try {
            for (id in songIds) {
                val cv =
                    ContentValues().apply {
                        put(COLUMN_PS_PLAYLIST_ID, playlistId)
                        put(COLUMN_PS_SONG_ID, id)
                        put(COLUMN_PS_ADDED_AT, System.currentTimeMillis() / 1000)
                    }
                val result =
                    db.insertWithOnConflict(
                        TABLE_PLAYLIST_SONGS,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_IGNORE,
                    )
                if (result != -1L) {
                    addedCount++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return addedCount
    }

    fun removeSongsFromPlaylistBatch(playlistId: Long, songIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in songIds) {
                db.delete(
                    TABLE_PLAYLIST_SONGS,
                    "$COLUMN_PS_PLAYLIST_ID = ? AND $COLUMN_PS_SONG_ID = ?",
                    arrayOf(playlistId.toString(), id.toString()),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteSongsBatch(songIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in songIds) {
                db.delete(TABLE_SONGS, "$COLUMN_ID = ?", arrayOf(id.toString()))
                db.delete(TABLE_PLAYLIST_SONGS, "$COLUMN_PS_SONG_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
