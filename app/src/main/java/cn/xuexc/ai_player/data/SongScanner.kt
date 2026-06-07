package cn.xuexc.ai_player.data

import android.content.Context
import android.provider.MediaStore

object SongScanner {
    fun isPathBlocked(filePath: String, blockedFolders: Set<String>): Boolean {
        if (blockedFolders.isEmpty()) return false
        val standardBlocked = blockedFolders.map {
            try { java.io.File(it).absolutePath } catch (e: Exception) { it }
        }
        return isPathBlocked(filePath, standardBlocked)
    }

    fun isPathBlocked(filePath: String, standardBlockedFolders: List<String>): Boolean {
        if (standardBlockedFolders.isEmpty()) return false
        val lastSlash = maxOf(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'))
        if (lastSlash <= 0) return false
        val parentPath = filePath.substring(0, lastSlash)
        return try {
            val standardParent = java.io.File(parentPath).absolutePath
            val separator = java.io.File.separator
            standardBlockedFolders.any { standardBlocked ->
                standardParent == standardBlocked || standardParent.startsWith(standardBlocked + separator)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun scanSongs(context: Context, blockedFolders: Set<String> = emptySet()): List<Song> {
        val songsList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Filter: Only music files, and length >= 10 seconds (10000ms) to filter out ringtones and system notification sounds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val path = cursor.getString(dataCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val dateAdded = cursor.getLong(dateAddedCol)

                    // Do not skip blocked folders during physical scan so that DB retains all songs.
                    // Dynamic filtering is handled in loadSongs/detectedFolders to support 0-latency unblocking.

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            path = path,
                            duration = duration,
                            size = size,
                            albumId = albumId,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songsList
    }
}
