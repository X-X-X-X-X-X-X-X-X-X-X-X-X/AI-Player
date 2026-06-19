package cn.xuexc.ai_player.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    val dateAdded: Long = 0L,
    val lastModified: Long = 0L,
)

// 全局内存缓存，使用最大可用内存的 1/8 作为上限（最少 16MB），按照 Bitmap 实际字节大小计重淘汰，根治 OOM 隐患
private val maxCacheSize =
    (Runtime.getRuntime().maxMemory() / 8).coerceAtLeast(1024 * 1024 * 16).toInt()

private data class CacheKey(val songId: Long, val size: Int)

private val coverCache =
    object : android.util.LruCache<CacheKey, android.graphics.Bitmap>(maxCacheSize) {
        override fun sizeOf(key: CacheKey?, value: android.graphics.Bitmap?): Int {
            return value?.byteCount ?: 0
        }
    }

// 并发信号量限流，防止快速滑动歌曲列表时，同时启动过多的底层 Native 图片解码导致 CPU 满载及 UI 顿挫
private val decodeSemaphore = Semaphore(3)

/** 辅助方法：计算最适合目标大小的 inSampleSize */
private fun calculateInSampleSize(
    options: android.graphics.BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/** 辅助方法：根据歌曲 ID 从 MediaStore 获取其对应的本地文件物理路径 */
private fun getSongPathById(context: android.content.Context, songId: Long): String? {
    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
    val selection = "${android.provider.MediaStore.Audio.Media._ID} = ?"
    val selectionArgs = arrayOf(songId.toString())
    try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor
            ->
            if (cursor.moveToFirst()) {
                val dataIndex =
                    cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                return cursor.getString(dataIndex)
            }
        }
    } catch (e: Exception) {
        // 忽略
    }
    return null
}

/**
 * 核心挂起方法：使用 MediaMetadataRetriever 从本地音频文件中提取内嵌的专辑图片数据并进行动态缩放解码 支持快速响应协程取消，避免不必要的 native 解码消耗 CPU。
 */
private suspend fun loadCoverFromPath(path: String, size: Int): android.graphics.Bitmap? {
    if (path.isEmpty()) return null
    val file = java.io.File(path)
    if (!file.exists()) return null

    currentCoroutineContext().ensureActive()
    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)

        currentCoroutineContext().ensureActive()
        val artBytes = retriever.embeddedPicture
        if (artBytes != null) {
            currentCoroutineContext().ensureActive()
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)

            currentCoroutineContext().ensureActive()
            opts.inSampleSize = calculateInSampleSize(opts, size, size)
            opts.inJustDecodeBounds = false

            currentCoroutineContext().ensureActive()
            return android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        // 忽略其他异常
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {}
    }
    return null
}

/** 核心同步方法：用于非挂起函数上下文（如同步服务拉取通知栏封面），同样享受动态 inSampleSize 优化 */
private fun loadCoverFromPathSync(path: String, size: Int): android.graphics.Bitmap? {
    if (path.isEmpty()) return null
    val file = java.io.File(path)
    if (!file.exists()) return null

    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        val artBytes = retriever.embeddedPicture
        if (artBytes != null) {
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
            opts.inSampleSize = calculateInSampleSize(opts, size, size)
            opts.inJustDecodeBounds = false
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

private fun getCoverCacheFile(
    context: android.content.Context,
    songId: Long,
    size: Int,
): java.io.File {
    val cacheDir = java.io.File(context.cacheDir, "covers")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return java.io.File(cacheDir, "${songId}_$size.jpg")
}

fun getCachedCoverById(songId: Long): android.graphics.Bitmap? {
    // 优先返回大尺寸缓存以保证清晰度，没有则返回小尺寸做占位
    return coverCache.get(CacheKey(songId, 400)) ?: coverCache.get(CacheKey(songId, 150))
}

/** API 1: 直接根据 Song 实例挂起加载文件内嵌封面 引入排队双检索与信号量控频，并在本地进行磁盘缓存，大幅降低 CPU 开销 */
suspend fun Song.loadCover(
    context: android.content.Context,
    size: Int = 150,
): android.graphics.Bitmap? {
    val key = CacheKey(this.id, size)
    // 1. 内存缓存检查
    val cached = coverCache.get(key)
    if (cached != null) return cached

    // 内存优化：若请求小图但内存已有大图，直接使用大图
    if (size <= 150) {
        val largerCached = coverCache.get(CacheKey(this.id, 400))
        if (largerCached != null) return largerCached
    }

    // 2. 磁盘缓存检查
    val cacheFile = getCoverCacheFile(context, this.id, size)
    if (cacheFile.exists()) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                coverCache.put(key, bitmap)
                return bitmap
            }
        } catch (e: Exception) {
            try {
                cacheFile.delete()
            } catch (ex: Exception) {}
        }
    }

    // 3. 原生文件解析与解码
    return decodeSemaphore.withPermit {
        // 进锁后 Double-Check 二次检查
        val doubleCheck = coverCache.get(key)
        if (doubleCheck != null) return@withPermit doubleCheck

        if (cacheFile.exists()) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    coverCache.put(key, bitmap)
                    return@withPermit bitmap
                }
            } catch (e: Exception) {
                try {
                    cacheFile.delete()
                } catch (ex: Exception) {}
            }
        }

        currentCoroutineContext().ensureActive()
        val loaded = loadCoverFromPath(this.path, size)
        if (loaded != null) {
            coverCache.put(key, loaded)
            try {
                java.io.FileOutputStream(cacheFile).use { out ->
                    loaded.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (e: Exception) {
                // 忽略磁盘写入失败
            }
        }
        loaded
    }
}

/** API 1 (Sync): 同步版本 API（用于不支持挂起函数的传统上下文） */
fun Song.loadCoverSync(
    context: android.content.Context,
    size: Int = 150,
): android.graphics.Bitmap? {
    val key = CacheKey(this.id, size)
    val cached = coverCache.get(key)
    if (cached != null) return cached

    if (size <= 150) {
        val largerCached = coverCache.get(CacheKey(this.id, 400))
        if (largerCached != null) return largerCached
    }

    val cacheFile = getCoverCacheFile(context, this.id, size)
    if (cacheFile.exists()) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                coverCache.put(key, bitmap)
                return bitmap
            }
        } catch (e: Exception) {
            try {
                cacheFile.delete()
            } catch (ex: Exception) {}
        }
    }

    val loaded = loadCoverFromPathSync(this.path, size)
    if (loaded != null) {
        coverCache.put(key, loaded)
        try {
            java.io.FileOutputStream(cacheFile).use { out ->
                loaded.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            // 忽略磁盘写入失败
        }
    }
    return loaded
}

/** API 2: 根据歌曲 ID 挂起加载内嵌封面 */
suspend fun loadCoverById(
    context: android.content.Context,
    songId: Long,
    size: Int = 150,
): android.graphics.Bitmap? {
    val key = CacheKey(songId, size)
    val cached = coverCache.get(key)
    if (cached != null) return cached

    if (size <= 150) {
        val largerCached = coverCache.get(CacheKey(songId, 400))
        if (largerCached != null) return largerCached
    }

    val cacheFile = getCoverCacheFile(context, songId, size)
    if (cacheFile.exists()) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                coverCache.put(key, bitmap)
                return bitmap
            }
        } catch (e: Exception) {
            try {
                cacheFile.delete()
            } catch (ex: Exception) {}
        }
    }

    return decodeSemaphore.withPermit {
        val doubleCheck = coverCache.get(key)
        if (doubleCheck != null) return@withPermit doubleCheck

        if (cacheFile.exists()) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    coverCache.put(key, bitmap)
                    return@withPermit bitmap
                }
            } catch (e: Exception) {
                try {
                    cacheFile.delete()
                } catch (ex: Exception) {}
            }
        }

        currentCoroutineContext().ensureActive()
        val path = getSongPathById(context, songId) ?: return@withPermit null

        currentCoroutineContext().ensureActive()
        val loaded = loadCoverFromPath(path, size)
        if (loaded != null) {
            coverCache.put(key, loaded)
            try {
                java.io.FileOutputStream(cacheFile).use { out ->
                    loaded.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (e: Exception) {
                // 忽略磁盘写入失败
            }
        }
        loaded
    }
}

/** 辅助方法：在主线程同步获取封面 LruCache 缓存的 Bitmap，实现 0 延迟首帧渲染 */
fun Song.getCachedCover(): android.graphics.Bitmap? {
    // 优先返回大尺寸缓存以保证清晰度，没有则返回小尺寸做占位
    return coverCache.get(CacheKey(this.id, 400)) ?: coverCache.get(CacheKey(this.id, 150))
}

/** 辅助方法：获取预模糊处理过的背景封面缓存 */
fun Song.getCachedBlurredCover(): android.graphics.Bitmap? {
    return coverCache.get(CacheKey(this.id, -1))
}

/** 快速双通道 BoxBlur 模糊算法，专为超小尺寸优化，执行效率极高 */
private fun blurBitmap(sentBitmap: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
    val bitmap =
        sentBitmap.copy(sentBitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val temp = IntArray(w * h)
    // 水平方向 BoxBlur
    for (y in 0 until h) {
        for (x in 0 until w) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dx in -radius..radius) {
                val nx = x + dx
                if (nx in 0 until w) {
                    val p = pix[y * w + nx]
                    r += (p shr 16) and 0xff
                    g += (p shr 8) and 0xff
                    b += p and 0xff
                    count++
                }
            }
            temp[y * w + x] =
                ((r / count) shl 16) or ((g / count) shl 8) or (b / count) or (0xff shl 24)
        }
    }

    // 垂直方向 BoxBlur
    for (x in 0 until w) {
        for (y in 0 until h) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dy in -radius..radius) {
                val ny = y + dy
                if (ny in 0 until h) {
                    val p = temp[ny * w + x]
                    r += (p shr 16) and 0xff
                    g += (p shr 8) and 0xff
                    b += p and 0xff
                    count++
                }
            }
            pix[y * w + x] =
                ((r / count) shl 16) or ((g / count) shl 8) or (b / count) or (0xff shl 24)
        }
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return bitmap
}

/** 挂起加载预模糊的背景封面图，处理 Lru 内存缓存和磁盘缓存 */
suspend fun Song.loadBlurredCover(
    context: android.content.Context,
    sourceBitmap: android.graphics.Bitmap,
): android.graphics.Bitmap? {
    val key = CacheKey(this.id, -1)
    val cached = coverCache.get(key)
    if (cached != null) return cached

    val cacheFile =
        java.io.File(context.cacheDir, "covers").let {
            if (!it.exists()) it.mkdirs()
            java.io.File(it, "${this.id}_blurred.jpg")
        }
    if (cacheFile.exists()) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                coverCache.put(key, bitmap)
                return bitmap
            }
        } catch (e: Exception) {
            try {
                cacheFile.delete()
            } catch (ex: Exception) {}
        }
    }

    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            // 缩放到 40x40 极大降低模糊运算负担，并利用双线性拉伸取得极佳的柔和模糊背景
            val scaled = android.graphics.Bitmap.createScaledBitmap(sourceBitmap, 40, 40, true)
            val blurred = blurBitmap(blurBitmap(scaled, 4), 4)
            coverCache.put(key, blurred)
            try {
                java.io.FileOutputStream(cacheFile).use { out ->
                    blurred.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (e: Exception) {}
            blurred
        } catch (e: Exception) {
            null
        }
    }
}

enum class QualityType {
    HiRes,
    HQ,
    SQ,
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

fun clearCoverCacheForSong(context: android.content.Context, songId: Long) {
    coverCache.remove(CacheKey(songId, 150))
    coverCache.remove(CacheKey(songId, 400))
    coverCache.remove(CacheKey(songId, -1))

    val cacheDir = java.io.File(context.cacheDir, "covers")
    if (cacheDir.exists()) {
        try {
            java.io.File(cacheDir, "${songId}_150.jpg").delete()
            java.io.File(cacheDir, "${songId}_400.jpg").delete()
            java.io.File(cacheDir, "${songId}_blurred.jpg").delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
