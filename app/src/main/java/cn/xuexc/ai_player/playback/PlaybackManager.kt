package cn.xuexc.ai_player.playback

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import cn.xuexc.ai_player.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlayMode {
    ListLoop,
    SingleLoop,
    Shuffle,
}

object PlaybackManager {
    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null
    private var isSeeking = false

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress

    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue: StateFlow<List<Song>> = _playbackQueue

    private val _playMode = MutableStateFlow(PlayMode.ListLoop)
    val playMode: StateFlow<PlayMode> = _playMode

    private var progressJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    val databaseUpdateEvent =
        kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining

    private val _sleepTimerPlayComplete = MutableStateFlow(false)
    val sleepTimerPlayComplete: StateFlow<Boolean> = _sleepTimerPlayComplete

    private var sleepTimerJob: Job? = null
    private var isTimerExpired = false

    private var originalQueue: List<Song> = emptyList()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        restoreState()
    }

    private fun saveState() {
        val ctx = appContext ?: return
        val sp = ctx.getSharedPreferences("playback_state_prefs", Context.MODE_PRIVATE)
        val editor = sp.edit()

        val songId = _currentSong.value?.id ?: -1L
        editor.putLong("current_song_id", songId)

        editor.putLong("playback_progress", _playbackProgress.value)

        val queueIds = _playbackQueue.value.map { it.id }.joinToString(",")
        editor.putString("playback_queue_ids", queueIds)

        val originalIds = originalQueue.map { it.id }.joinToString(",")
        editor.putString("original_queue_ids", originalIds)

        editor.putString("play_mode", _playMode.value.name)
        editor.apply()
    }

    private fun saveProgressOnly(pos: Long) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences("playback_state_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("playback_progress", pos)
            .apply()
    }

    private fun restoreState() {
        val ctx = appContext ?: return
        val sp = ctx.getSharedPreferences("playback_state_prefs", Context.MODE_PRIVATE)

        val modeStr = sp.getString("play_mode", PlayMode.ListLoop.name)
        _playMode.value =
            try {
                PlayMode.valueOf(modeStr ?: PlayMode.ListLoop.name)
            } catch (e: Exception) {
                PlayMode.ListLoop
            }

        coroutineScope.launch(Dispatchers.IO) {
            val dbHelper = cn.xuexc.ai_player.data.MusicDatabaseHelper(ctx)
            val allSongs = dbHelper.getAllSongs(includeBlacklisted = true)
            val songMap = allSongs.associateBy { it.id }

            val queueIdsStr = sp.getString("playback_queue_ids", "") ?: ""
            val originalIdsStr = sp.getString("original_queue_ids", "") ?: ""

            val queueIds =
                if (queueIdsStr.isNotBlank())
                    queueIdsStr.split(",").mapNotNull { it.toLongOrNull() }
                else emptyList()
            val originalIds =
                if (originalIdsStr.isNotBlank())
                    originalIdsStr.split(",").mapNotNull { it.toLongOrNull() }
                else emptyList()

            val restoredQueue = queueIds.mapNotNull { songMap[it] }
            val restoredOriginalQueue = originalIds.mapNotNull { songMap[it] }

            val currentSongId = sp.getLong("current_song_id", -1L)
            val restoredCurrentSong = songMap[currentSongId]

            val progress = sp.getLong("playback_progress", 0L)

            launch(Dispatchers.Main) {
                originalQueue = restoredOriginalQueue
                _playbackQueue.value = restoredQueue
                _currentSong.value = restoredCurrentSong
                _playbackProgress.value = progress
            }
        }
    }

    private fun reorderQueue(current: Song?) {
        val baseQueue = originalQueue.ifEmpty { _playbackQueue.value }
        if (baseQueue.isEmpty()) return

        val currentSongId = current?.id ?: baseQueue.first().id
        val currentIndex = baseQueue.indexOfFirst { it.id == currentSongId }
        val activeCurrent = if (currentIndex != -1) baseQueue[currentIndex] else baseQueue.first()

        val rest = baseQueue.filter { it.id != activeCurrent.id }

        val orderedRest =
            when (_playMode.value) {
                PlayMode.Shuffle -> {
                    rest.shuffled()
                }

                else -> {
                    val indexInBase = baseQueue.indexOfFirst { it.id == activeCurrent.id }
                    if (indexInBase != -1) {
                        baseQueue.subList(indexInBase + 1, baseQueue.size) +
                            baseQueue.subList(0, indexInBase)
                    } else {
                        rest
                    }
                }
            }

        _playbackQueue.value = listOf(activeCurrent) + orderedRest
    }

    fun setPlaybackQueue(queue: List<Song>) {
        originalQueue = queue
        if (_playMode.value == PlayMode.Shuffle) {
            reorderQueue(_currentSong.value)
        } else {
            _playbackQueue.value = queue
        }
        if (queue.isEmpty()) {
            stop(isSwitching = false)
        }
        saveState()
    }

    fun removeFromQueue(songId: Long): Song? {
        val current = _currentSong.value
        var nextSongToPlay: Song? = null

        if (current?.id == songId) {
            val queue = _playbackQueue.value
            val currentIndex = queue.indexOfFirst { it.id == songId }
            if (currentIndex != -1 && queue.size > 1) {
                val nextIndex = currentIndex % (queue.size - 1)
                val remainingQueue = queue.filter { it.id != songId }
                nextSongToPlay = remainingQueue.getOrNull(nextIndex)
            }
        }

        originalQueue = originalQueue.filter { it.id != songId }
        _playbackQueue.value = _playbackQueue.value.filter { it.id != songId }
        if (_playbackQueue.value.isEmpty()) {
            stop(isSwitching = false)
        }
        saveState()
        return nextSongToPlay
    }

    fun updateSongInQueue(songId: Long, isFavorite: Boolean) {
        originalQueue =
            originalQueue.map { if (it.id == songId) it.copy(isFavorite = isFavorite) else it }
        _playbackQueue.value =
            _playbackQueue.value.map {
                if (it.id == songId) it.copy(isFavorite = isFavorite) else it
            }
        saveState()
    }

    fun updateCurrentSongFavorite(isFavorite: Boolean) {
        _currentSong.value = _currentSong.value?.copy(isFavorite = isFavorite)
        saveState()
    }

    fun togglePlayMode() {
        _playMode.value =
            when (_playMode.value) {
                PlayMode.ListLoop -> PlayMode.SingleLoop
                PlayMode.SingleLoop -> PlayMode.Shuffle
                PlayMode.Shuffle -> PlayMode.ListLoop
            }
        reorderQueue(_currentSong.value)
        saveState()
    }

    fun playNext(context: Context, isUserInitiated: Boolean = false) {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) {
            stop()
            return
        }
        val current = _currentSong.value
        val nextSong =
            when (_playMode.value) {
                PlayMode.SingleLoop -> {
                    if (isUserInitiated) {
                        val currentIndex = queue.indexOfFirst { it.id == (current?.id ?: -1) }
                        val nextIndex =
                            if (currentIndex != -1) (currentIndex + 1) % queue.size else 0
                        queue[nextIndex]
                    } else {
                        current ?: queue[0]
                    }
                }

                else -> {
                    val currentIndex = queue.indexOfFirst { it.id == (current?.id ?: -1) }
                    val nextIndex = if (currentIndex != -1) (currentIndex + 1) % queue.size else 0
                    queue[nextIndex]
                }
            }
        playSong(context, nextSong, forceRestart = true)
    }

    fun playPrevious(context: Context) {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) {
            stop()
            return
        }
        val current = _currentSong.value
        val currentIndex = queue.indexOfFirst { it.id == (current?.id ?: -1) }
        val prevIndex =
            if (currentIndex != -1) (currentIndex - 1 + queue.size) % queue.size
            else queue.lastIndex
        val prevSong = queue[prevIndex]
        playSong(context, prevSong, forceRestart = true)
    }

    fun prepareNextWithoutPlaying(context: Context) {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) {
            stop()
            return
        }
        val current = _currentSong.value
        val nextSong =
            when (_playMode.value) {
                PlayMode.SingleLoop -> {
                    val currentIndex = queue.indexOfFirst { it.id == (current?.id ?: -1) }
                    val nextIndex = if (currentIndex != -1) (currentIndex + 1) % queue.size else 0
                    queue[nextIndex]
                }

                else -> {
                    val currentIndex = queue.indexOfFirst { it.id == (current?.id ?: -1) }
                    val nextIndex = if (currentIndex != -1) (currentIndex + 1) % queue.size else 0
                    queue[nextIndex]
                }
            }
        stop(isSwitching = true)
        _currentSong.value = nextSong
        _playbackProgress.value = 0L
        _isPlaying.value = false
        saveState()
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun playSong(
        context: Context,
        song: Song,
        forceRestart: Boolean = false,
        startPositionMs: Long = 0L,
    ) {
        appContext = context.applicationContext
        if (isTimerExpired) {
            resetSleepTimer()
        }
        if (!forceRestart && _currentSong.value?.id == song.id) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop(isSwitching = true)
        _currentSong.value = song
        _playbackProgress.value = startPositionMs
        saveState()

        try {
            _isPlaying.value = true
            mediaPlayer =
                MediaPlayer().apply {
                    if (song.path.startsWith("http")) {
                        setDataSource(song.path)
                    } else {
                        setDataSource(context, Uri.parse(song.path))
                    }
                    setOnPreparedListener {
                        if (startPositionMs > 0L) {
                            seekTo(startPositionMs)
                        }
                        if (_isPlaying.value) {
                            start()
                            startProgressTracker()
                        }
                        triggerServiceUpdate()
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackProgress.value = 0L
                        stopProgressTracker()

                        val isTimerActive = _sleepTimerRemaining.value > 0
                        val isCloseToExpiration =
                            isTimerActive && _sleepTimerRemaining.value <= 10000L

                        if (isTimerExpired || isCloseToExpiration) {
                            appContext?.let { ctx -> prepareNextWithoutPlaying(ctx) }
                            resetSleepTimer()
                        } else {
                            appContext?.let { ctx -> playNext(ctx, isUserInitiated = false) }
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlaying.value = false
                        stopProgressTracker()
                        android.widget.Toast.makeText(
                                context,
                                "播放失败: 本地无此音频文件",
                                android.widget.Toast.LENGTH_SHORT,
                            )
                            .show()
                        true
                    }
                    setOnSeekCompleteListener { isSeeking = false }
                    prepareAsync()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
            android.widget.Toast.makeText(
                    context,
                    "播放失败: 本地无此音频文件",
                    android.widget.Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    fun pause() {
        _isPlaying.value = false
        stopProgressTracker()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        triggerServiceUpdate()
        saveState()
    }

    fun resume() {
        val song = _currentSong.value
        if (mediaPlayer == null && song != null) {
            appContext?.let { ctx ->
                playSong(ctx, song, forceRestart = true, startPositionMs = _playbackProgress.value)
            }
        } else {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
                triggerServiceUpdate()
                saveState()
            }
        }
    }

    fun stop(isSwitching: Boolean = false) {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        stopProgressTracker()
        isSeeking = false
        if (!isSwitching) {
            _playbackProgress.value = 0L
            _currentSong.value = null
            appContext?.let { ctx ->
                val intent = Intent(ctx, PlaybackService::class.java)
                ctx.stopService(intent)
            }
        }
        saveState()
    }

    private fun triggerServiceUpdate() {
        appContext?.let { ctx ->
            val intent =
                Intent(ctx, PlaybackService::class.java).apply {
                    action = PlaybackService.ACTION_UPDATE
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        isSeeking = true
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackProgress.value = positionMs
        triggerServiceUpdate()
        saveState()
    }

    fun toggleFavoriteCurrent(context: Context) {
        val song = _currentSong.value ?: return
        val targetFav = !song.isFavorite

        // 乐观更新内存中的状态，并立即触发通知栏重绘，实现 0 延迟响应
        updateCurrentSongFavorite(targetFav)
        updateSongInQueue(song.id, targetFav)
        triggerServiceUpdate()

        coroutineScope.launch(Dispatchers.IO) {
            val dbHelper = cn.xuexc.ai_player.data.MusicDatabaseHelper(context)
            dbHelper.setFavorite(song.id, targetFav)
            launch(Dispatchers.Main) {
                databaseUpdateEvent.tryEmit(Unit)
                val msg = if (targetFav) "已添加到喜欢" else "已取消喜欢"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun blacklistCurrent(context: Context) {
        val song = _currentSong.value ?: return

        // 乐观更新：立刻将其从当前队列移除，并切换到下一首，达到即时切歌效果
        val nextSong = removeFromQueue(song.id)
        if (nextSong != null) {
            playSong(context, nextSong, forceRestart = true)
        }

        coroutineScope.launch(Dispatchers.IO) {
            val dbHelper = cn.xuexc.ai_player.data.MusicDatabaseHelper(context)
            dbHelper.setBlacklisted(song.id, true)
            launch(Dispatchers.Main) { databaseUpdateEvent.tryEmit(Unit) }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob =
            coroutineScope.launch {
                var saveCounter = 0
                while (true) {
                    mediaPlayer?.let {
                        if (it.isPlaying && !isSeeking) {
                            val pos = it.currentPosition.toLong()
                            _playbackProgress.value = pos
                            saveCounter++
                            if (saveCounter >= 25) { // 5秒保存一次 (25 * 200ms)
                                saveCounter = 0
                                saveProgressOnly(pos)
                            }
                        }
                    }
                    delay(200)
                }
            }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun startSleepTimer(durationMs: Long, playComplete: Boolean) {
        sleepTimerJob?.cancel()
        isTimerExpired = false
        _sleepTimerPlayComplete.value = playComplete
        _sleepTimerRemaining.value = durationMs

        sleepTimerJob =
            coroutineScope.launch {
                while (_sleepTimerRemaining.value > 0) {
                    delay(1000)
                    val nextVal = _sleepTimerRemaining.value - 1000
                    _sleepTimerRemaining.value = maxOf(0L, nextVal)
                }
                if (playComplete) {
                    isTimerExpired = true
                    if (!_isPlaying.value) {
                        resetSleepTimer()
                    }
                } else {
                    pause()
                    appContext?.let { ctx ->
                        ctx.stopService(Intent(ctx, PlaybackService::class.java))
                    }
                    resetSleepTimer()
                }
            }
    }

    fun cancelSleepTimer() {
        resetSleepTimer()
    }

    private fun resetSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        isTimerExpired = false
        _sleepTimerRemaining.value = 0L
        _sleepTimerPlayComplete.value = false
    }
}
