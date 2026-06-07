package cn.xuexc.ai_player.data

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val albumId: Long,
    val isFavorite: Boolean = false,
    val isBlacklisted: Boolean = false,
    val dateAdded: Long = 0L
)

// 全局内存缓存，提高 LazyColumn 滑动性能
private val coverCache = android.util.LruCache<Long, android.graphics.Bitmap>(100)

/**
 * 辅助方法：根据歌曲 ID 从 MediaStore 获取其对应的本地文件物理路径
 */
private fun getSongPathById(context: android.content.Context, songId: Long): String? {
    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
    val selection = "${android.provider.MediaStore.Audio.Media._ID} = ?"
    val selectionArgs = arrayOf(songId.toString())
    try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                return cursor.getString(dataIndex)
            }
        }
    } catch (e: Exception) {
        // 忽略
    }
    return null
}

/**
 * 核心方法：使用 MediaMetadataRetriever 从本地音频文件中直接提取内嵌的专辑图片数据 (ID3 APIC)
 * 100% 确保图片属于这首歌曲自身，彻底根治 MediaStore 串歌封面的系统 Bug
 */
private fun loadCoverFromPath(path: String, size: Int): android.graphics.Bitmap? {
    if (path.isEmpty()) return null
    val file = java.io.File(path)
    if (!file.exists()) return null

    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        val artBytes = retriever.embeddedPicture
        if (artBytes != null) {
            val opts = android.graphics.BitmapFactory.Options()
            if (size < 200) {
                opts.inSampleSize = 2 // 针对列表小封面，缩小图像以节省内存
            }
            return android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
        }
    } catch (e: Exception) {
        // 忽略
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {}
    }
    return null
}

/**
 * API 1: 直接根据 Song 实例加载文件内嵌封面（避免了 contentResolver 路径查询，性能最高）
 */
fun Song.loadCover(context: android.content.Context, size: Int = 150): android.graphics.Bitmap? {
    val cached = coverCache.get(this.id)
    if (cached != null) return cached

    val loaded = loadCoverFromPath(this.path, size)
    if (loaded != null) {
        coverCache.put(this.id, loaded)
    }
    return loaded
}

/**
 * API 2: 根据歌曲 ID 加载内嵌封面（适用于没有 Song 实例的场景，如 PlaylistCover 歌单封面）
 */
fun loadCoverById(context: android.content.Context, songId: Long, size: Int = 150): android.graphics.Bitmap? {
    val cached = coverCache.get(songId)
    if (cached != null) return cached

    val path = getSongPathById(context, songId) ?: return null
    val loaded = loadCoverFromPath(path, size)
    if (loaded != null) {
        coverCache.put(songId, loaded)
    }
    return loaded
}

/**
 * 辅助方法：在主线程同步获取封面 LruCache 缓存的 Bitmap，实现 0 延迟首帧渲染
 */
fun Song.getCachedCover(): android.graphics.Bitmap? {
    return coverCache.get(this.id)
}

enum class QualityType {
    HiRes, HQ, SQ
}

fun Song.getQualityType(): QualityType {
    val ext = path.substringAfterLast('.', "").lowercase()
    if (ext in listOf("flac", "wav", "ape", "dsd", "dff", "dsf", "alac")) {
        return QualityType.HiRes
    }
    if (duration > 0) {
        val kbps = (size * 8) / duration
        if (kbps >= 500) {
            return QualityType.HiRes
        }
        if (kbps >= 250) {
            return QualityType.HQ
        }
    }
    return QualityType.SQ
}

