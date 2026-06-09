package cn.xuexc.ai_player.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xuexc.ai_player.data.*
import cn.xuexc.ai_player.playback.PlayMode
import cn.xuexc.ai_player.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder(val displayName: String) {
    BY_TITLE("按名称排序"),
    BY_ADD_TIME("按扫描时间排序"),
    BY_DATE_ADDED("按创建时间排序"),
    BY_ARTIST("按歌手排序"),
    BY_DURATION("按时长排序")
}

sealed interface ScanStatus {
    object Idle : ScanStatus
    object Scanning : ScanStatus
    data class Success(val count: Int) : ScanStatus
    data class Error(val message: String) : ScanStatus
}

data class ImportResult(
    val playlistsImported: Int,
    val successCount: Int,
    val failureCount: Int,
    val failedSongs: List<String>
)

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = MusicDatabaseHelper(application)

    // Theme accent color persistence
    private val sharedPrefs = application.getSharedPreferences("ai_player_prefs", Context.MODE_PRIVATE)

    private val _blockedFolders =
        MutableStateFlow(sharedPrefs.getStringSet("blocked_folders", emptySet()) ?: emptySet())
    val blockedFolders: StateFlow<Set<String>> = _blockedFolders

    fun addBlockedFolder(path: String) {
        val updated = _blockedFolders.value.toMutableSet().apply { add(path.trim()) }
        _blockedFolders.value = updated
        sharedPrefs.edit().putStringSet("blocked_folders", updated).apply()
        loadSongs()
        loadPlaylists()
    }

    fun removeBlockedFolder(path: String) {
        val updated = _blockedFolders.value.toMutableSet().apply { remove(path) }
        _blockedFolders.value = updated
        sharedPrefs.edit().putStringSet("blocked_folders", updated).apply()
        loadSongs()
        loadPlaylists()
        // 静默扫描文件系统以补回先前屏蔽期间被库物理清除的历史音轨
        startScan(getApplication(), isSilent = true)
    }

    private val _themeName =
        MutableStateFlow(sharedPrefs.getString("theme_accent_name", "TitaniumGray") ?: "TitaniumGray")
    val themeName: StateFlow<String> = _themeName

    fun setThemeName(name: String) {
        _themeName.value = name
        sharedPrefs.edit().putString("theme_accent_name", name).apply()
    }

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("theme_is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        sharedPrefs.edit().putBoolean("theme_is_dark_mode", dark).apply()
    }

    private val _sortOrder = MutableStateFlow(
        SortOrder.valueOf(
            sharedPrefs.getString("song_sort_order", SortOrder.BY_DATE_ADDED.name) ?: SortOrder.BY_DATE_ADDED.name
        )
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        sharedPrefs.edit().putString("song_sort_order", order.name).apply()
        loadSongs()
    }

    private val _isSortAscending = MutableStateFlow(sharedPrefs.getBoolean("song_sort_ascending", false))
    val isSortAscending: StateFlow<Boolean> = _isSortAscending

    fun setSortAscending(ascending: Boolean) {
        _isSortAscending.value = ascending
        sharedPrefs.edit().putBoolean("song_sort_ascending", ascending).apply()
        loadSongs()
    }

    private val _scanState = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanState: StateFlow<ScanStatus> = _scanState

    // Playback state forwarded from PlaybackManager
    val currentSong: StateFlow<Song?> = PlaybackManager.currentSong
    val isPlaying: StateFlow<Boolean> = PlaybackManager.isPlaying
    val playbackProgress: StateFlow<Long> = PlaybackManager.playbackProgress
    val playMode: StateFlow<PlayMode> = PlaybackManager.playMode
    val playbackQueue: StateFlow<List<Song>> = PlaybackManager.playbackQueue
    val sleepTimerRemaining: StateFlow<Long> = PlaybackManager.sleepTimerRemaining
    val sleepTimerPlayComplete: StateFlow<Boolean> = PlaybackManager.sleepTimerPlayComplete

    private val _allScannedSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Filtered songs based on search query
    val songs: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedFolders: StateFlow<List<String>> = combine(
        _allScannedSongs,
        _blockedFolders
    ) { allSongs, blocked ->
        val standardBlocked = blocked.map {
            try {
                java.io.File(it).absolutePath
            } catch (e: Exception) {
                it
            }
        }
        allSongs.mapNotNull { song ->
            if (!song.path.startsWith("http://") && !song.path.startsWith("https://")) {
                val lastSlash = maxOf(song.path.lastIndexOf('/'), song.path.lastIndexOf('\\'))
                val parent = if (lastSlash > 0) song.path.substring(0, lastSlash) else null
                if (parent != null && !SongScanner.isPathBlocked(song.path, standardBlocked)) {
                    parent
                } else {
                    null
                }
            } else {
                null
            }
        }.distinct().sorted()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryDisplayData: StateFlow<LibraryDisplayData> = combine(
        songs,
        _sortOrder,
        _isSortAscending
    ) { filteredSongs, sortOrder, isSortAscending ->
        if (sortOrder == SortOrder.BY_TITLE) {
            val mapped = filteredSongs.map { song ->
                val letter = PinyinHelper.getArtistLetter(song.title)
                SongItemWithLetter(song = song, letter = letter)
            }
            val sorted = if (isSortAscending) {
                mapped.sortedWith(
                    compareBy<SongItemWithLetter> { if (it.letter == '#') 'Z' + 1 else it.letter }
                        .thenBy { it.song.title }
                )
            } else {
                mapped.sortedWith(
                    compareByDescending<SongItemWithLetter> { if (it.letter == '#') 'A' - 1 else it.letter }
                        .thenByDescending { it.song.title }
                )
            }
            val list = mutableListOf<Any>()
            val indexMap = mutableMapOf<Char, Int>()
            val grouped = sorted.groupBy { it.letter }
            grouped.forEach { (letter, items) ->
                indexMap[letter] = list.size
                list.add(letter)
                list.addAll(items)
            }
            LibraryDisplayData(list, indexMap)
        } else {
            LibraryDisplayData(emptyList(), emptyMap())
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryDisplayData())

    val artistDisplayData: StateFlow<ArtistDisplayData> = songs.map { filteredSongs ->
        val groupedByArtist = filteredSongs.groupBy { it.artist }
        val mapped = groupedByArtist.map { (artist, songList) ->
            val displayName = if (artist.isBlank()) "未知歌手" else artist
            val letter = PinyinHelper.getArtistLetter(displayName)
            ArtistItem(
                artistKey = artist,
                displayName = displayName,
                songs = songList,
                letter = letter
            )
        }
        val sorted = mapped.sortedWith(
            compareBy<ArtistItem> { if (it.letter == '#') 'Z' + 1 else it.letter }
                .thenBy { it.displayName }
        )
        val list = mutableListOf<Any>()
        val indexMap = mutableMapOf<Char, Int>()
        val groupedArtists = sorted.groupBy { it.letter }
        groupedArtists.forEach { (letter, items) ->
            indexMap[letter] = list.size
            list.add(letter)
            list.addAll(items)
        }
        ArtistDisplayData(list, indexMap)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArtistDisplayData())

    // --- Playlist States ---
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    val favoriteSongsCount = MutableStateFlow(0)
    val blacklistSongsCount = MutableStateFlow(0)

    private val _isSongsLoaded = MutableStateFlow(false)
    val isSongsLoaded: StateFlow<Boolean> =
        combine(_isSongsLoaded, songs, _allSongs) { loaded, songsList, allSongsList ->
            loaded && (songsList.isNotEmpty() || allSongsList.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isPlaylistsLoaded = MutableStateFlow(false)
    val isPlaylistsLoaded: StateFlow<Boolean> = _isPlaylistsLoaded

    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylistSongs: StateFlow<List<Song>> = _currentPlaylistSongs

    init {
        PlaybackManager.initialize(application)
        viewModelScope.launch {
            PlaybackManager.databaseUpdateEvent.collect {
                loadSongs()
                loadPlaylists()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val demoSongs = listOf(
                Song(
                    999991,
                    "Acoustic Breeze",
                    "Bensound",
                    "Bensound Free Music",
                    "https://www.bensound.com/bensound-music/bensound-acousticbreeze.mp3",
                    156000,
                    3120000,
                    1,
                    dateAdded = 1000L
                ),
                Song(
                    999992,
                    "Sunny",
                    "Bensound",
                    "Bensound Free Music",
                    "https://www.bensound.com/bensound-music/bensound-sunny.mp3",
                    140000,
                    2800000,
                    2,
                    dateAdded = 2000L
                ),
                Song(
                    999993,
                    "Little Idea",
                    "Bensound",
                    "Bensound Free Music",
                    "https://www.bensound.com/bensound-music/bensound-littleidea.mp3",
                    169000,
                    3380000,
                    3,
                    dateAdded = 3000L
                )
            )
            val existing = dbHelper.getAllSongs(includeBlacklisted = true)
            var needsSync = false
            for (song in demoSongs) {
                val found = existing.firstOrNull { it.id == song.id }
                if (found != null && found.dateAdded == 0L) {
                    needsSync = true
                    break
                }
            }
            if (needsSync) {
                dbHelper.syncSongs(demoSongs)
                loadSongs()
            }
        }
        loadSongs()
        loadPlaylists()
    }

    fun loadSongs() {
        viewModelScope.launch {
            loadSongsInternal()
        }
    }

    private suspend fun loadSongsInternal() = withContext(Dispatchers.IO) {
        val ascDesc = if (_isSortAscending.value) "ASC" else "DESC"
        val orderClause = when (_sortOrder.value) {
            SortOrder.BY_TITLE -> "title $ascDesc"
            SortOrder.BY_ADD_TIME -> "id $ascDesc"
            SortOrder.BY_DATE_ADDED -> "date_added $ascDesc"
            SortOrder.BY_ARTIST -> "artist $ascDesc, title $ascDesc"
            SortOrder.BY_DURATION -> "duration $ascDesc"
        }
        val list = dbHelper.getAllSongs(includeBlacklisted = false, orderBy = orderClause)
        _allScannedSongs.value = list
        val blocked = _blockedFolders.value
        val standardBlocked = blocked.map {
            try {
                java.io.File(it).absolutePath
            } catch (e: Exception) {
                it
            }
        }
        _allSongs.value = list.filter { !SongScanner.isPathBlocked(it.path, standardBlocked) }
        _isSongsLoaded.value = true
    }

    fun startScan(context: Context, isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _scanState.value = ScanStatus.Scanning
            }
            try {
                val blocked = _blockedFolders.value
                val scanned = withContext(Dispatchers.IO) {
                    var songsList = SongScanner.scanSongs(context, blocked)
                    if (songsList.isEmpty()) {
                        // Inject 3 demo songs for easy testing in cases where the emulator/device doesn't have any physical audio files
                        songsList = listOf(
                            Song(
                                999991,
                                "Acoustic Breeze",
                                "Bensound",
                                "Bensound Free Music",
                                "https://www.bensound.com/bensound-music/bensound-acousticbreeze.mp3",
                                156000,
                                3120000,
                                1,
                                dateAdded = 1000L
                            ),
                            Song(
                                999992,
                                "Sunny",
                                "Bensound",
                                "Bensound Free Music",
                                "https://www.bensound.com/bensound-music/bensound-sunny.mp3",
                                140000,
                                2800000,
                                2,
                                dateAdded = 2000L
                            ),
                            Song(
                                999993,
                                "Little Idea",
                                "Bensound",
                                "Bensound Free Music",
                                "https://www.bensound.com/bensound-music/bensound-littleidea.mp3",
                                169000,
                                3380000,
                                3,
                                dateAdded = 3000L
                            )
                        )
                    }
                    dbHelper.syncSongs(songsList)
                    songsList
                }
                loadSongsInternal()
                loadPlaylistsInternal()
                if (!isSilent) {
                    _scanState.value = ScanStatus.Success(_allSongs.value.size)
                }
            } catch (e: Exception) {
                if (!isSilent) {
                    _scanState.value = ScanStatus.Error(e.localizedMessage ?: "Unknown scanning error")
                }
            }
        }
    }

    fun toggleFavorite(song: Song, skipListUpdate: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetFav = !song.isFavorite
            dbHelper.setFavorite(song.id, targetFav)

            // 同步修改内存中当前歌曲的 favorite 状态以实现 UI 的即时反应
            if (PlaybackManager.currentSong.value?.id == song.id) {
                PlaybackManager.updateCurrentSongFavorite(targetFav)
            }

            // 同步更新播放队列及原始队列中的歌曲收藏状态
            PlaybackManager.updateSongInQueue(song.id, targetFav)

            if (!skipListUpdate) {
                // 仅在非播放界面（即主界面列表）点击时更新大列表，触发即时局部重组
                _allScannedSongs.value = _allScannedSongs.value.map {
                    if (it.id == song.id) it.copy(isFavorite = targetFav) else it
                }
                _allSongs.value = _allSongs.value.map {
                    if (it.id == song.id) it.copy(isFavorite = targetFav) else it
                }
                // 当取消喜欢时，直接从喜欢歌单列表中移除该歌曲，无需依赖外部 loadFavoriteSongs()
                // 当添加喜欢时，仅更新 isFavorite 字段（该歌曲不在喜欢歌单当前页，不需插入）
                if (!targetFav) {
                    _currentPlaylistSongs.value = _currentPlaylistSongs.value.filter { it.id != song.id }
                } else {
                    _currentPlaylistSongs.value = _currentPlaylistSongs.value.map {
                        if (it.id == song.id) it.copy(isFavorite = true) else it
                    }
                }
            }
            favoriteSongsCount.value =
                if (targetFav) favoriteSongsCount.value + 1 else maxOf(0, favoriteSongsCount.value - 1)

            withContext(Dispatchers.Main) {
                val msg = if (targetFav) "已添加到喜欢" else "已取消喜欢"
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addToBlacklist(context: Context, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setBlacklisted(song.id, true)
            withContext(Dispatchers.Main) {
                val isCurrentPlaying = currentSong.value?.id == song.id
                val nextSong = PlaybackManager.removeFromQueue(song.id)
                if (isCurrentPlaying && nextSong != null) {
                    PlaybackManager.playSong(context, nextSong, forceRestart = true)
                }
                loadSongs()
                loadPlaylists()
                if (isCurrentPlaying) {
                    Toast.makeText(context, "已移入遗忘的沙漏，自动切歌", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "已移入遗忘的沙漏", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetScanState() {
        _scanState.value = ScanStatus.Idle
    }

    // --- Playback Delegates ---

    fun setPlaybackQueue(queue: List<Song>) {
        PlaybackManager.setPlaybackQueue(queue)
    }

    fun togglePlayMode() {
        PlaybackManager.togglePlayMode()
    }

    fun playNextSong(context: Context, isUserInitiated: Boolean = true) {
        PlaybackManager.playNext(context, isUserInitiated)
    }

    fun playPreviousSong(context: Context) {
        PlaybackManager.playPrevious(context)
    }

    fun playSong(context: Context, song: Song) {
        PlaybackManager.playSong(context, song)
    }

    fun pauseSong() {
        PlaybackManager.pause()
    }

    fun resumeSong() {
        PlaybackManager.resume()
    }

    fun startSleepTimer(durationMs: Long, playComplete: Boolean) {
        PlaybackManager.startSleepTimer(durationMs, playComplete)
    }

    fun cancelSleepTimer() {
        PlaybackManager.cancelSleepTimer()
    }

    fun seekTo(positionMs: Long) {
        PlaybackManager.seekTo(positionMs)
    }

    fun removeFromPlaybackQueue(songId: Long, context: Context) {
        val current = currentSong.value
        val nextSong = PlaybackManager.removeFromQueue(songId)
        if (current?.id == songId && nextSong != null) {
            PlaybackManager.playSong(context, nextSong, forceRestart = true)
        }
    }

    // --- Playlist Operations ---

    fun loadPlaylists() {
        viewModelScope.launch {
            loadPlaylistsInternal()
        }
    }

    private suspend fun loadPlaylistsInternal() = withContext(Dispatchers.IO) {
        val list = dbHelper.getPlaylists()
        val blocked = _blockedFolders.value
        val standardBlocked = blocked.map {
            try {
                java.io.File(it).absolutePath
            } catch (e: Exception) {
                it
            }
        }

        // Filter favorite songs count
        val favoriteSongs = dbHelper.getFavoriteSongs()
        favoriteSongsCount.value = favoriteSongs.count { !SongScanner.isPathBlocked(it.path, standardBlocked) }

        // Filter blacklist songs count
        val blacklistSongs = dbHelper.getBlacklistedSongs()
        blacklistSongsCount.value = blacklistSongs.count { !SongScanner.isPathBlocked(it.path, standardBlocked) }

        // Filter custom playlists counts
        val updatedPlaylists = list.map { playlist ->
            val songsInPlaylist = dbHelper.getSongsInPlaylist(playlist.id)
            val filteredSongs = songsInPlaylist.filter { !SongScanner.isPathBlocked(it.path, standardBlocked) }
            playlist.copy(
                songCount = filteredSongs.size,
                firstSongId = filteredSongs.firstOrNull()?.id
            )
        }

        _playlists.value = updatedPlaylists
        _isPlaylistsLoaded.value = true
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.createPlaylist(name)
            loadPlaylists()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deletePlaylist(playlistId)
            loadPlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.addSongToPlaylist(playlistId, songId)
            loadPlaylists()
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.removeSongFromPlaylist(playlistId, songId)
            loadPlaylists()
            loadSongsInPlaylist(playlistId)
        }
    }

    fun loadSongsInPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getSongsInPlaylist(playlistId)
            val blocked = _blockedFolders.value
            val standardBlocked = blocked.map {
                try {
                    java.io.File(it).absolutePath
                } catch (e: Exception) {
                    it
                }
            }
            _currentPlaylistSongs.value = list.filter { !SongScanner.isPathBlocked(it.path, standardBlocked) }
        }
    }

    fun loadFavoriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getFavoriteSongs()
            val blocked = _blockedFolders.value
            val standardBlocked = blocked.map {
                try {
                    java.io.File(it).absolutePath
                } catch (e: Exception) {
                    it
                }
            }
            _currentPlaylistSongs.value = list.filter { !SongScanner.isPathBlocked(it.path, standardBlocked) }
        }
    }

    fun loadBlacklistSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dbHelper.getBlacklistedSongs()
            val blocked = _blockedFolders.value
            val standardBlocked = blocked.map {
                try {
                    java.io.File(it).absolutePath
                } catch (e: Exception) {
                    it
                }
            }
            _currentPlaylistSongs.value = list.filter { !SongScanner.isPathBlocked(it.path, standardBlocked) }
        }
    }

    fun removeFromBlacklist(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setBlacklisted(song.id, false)
            loadSongs()
            loadPlaylists()
            loadBlacklistSongs()
        }
    }

    fun deleteSong(context: Context, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteSong(songId)
            val current = PlaybackManager.currentSong.value
            val nextSong = withContext(Dispatchers.Main) {
                PlaybackManager.removeFromQueue(songId)
            }
            if (current?.id == songId && nextSong != null) {
                withContext(Dispatchers.Main) {
                    PlaybackManager.playSong(context, nextSong, forceRestart = true)
                }
            }
            loadSongsInternal()
            loadPlaylistsInternal()
            val currentPlaylist = _currentPlaylistSongs.value
            if (currentPlaylist.any { it.id == songId }) {
                _currentPlaylistSongs.value = currentPlaylist.filter { it.id != songId }
            }
        }
    }

    suspend fun exportPlaylistsJson(): String = withContext(Dispatchers.IO) {
        val jsonArray = org.json.JSONArray()

        // 1. 导出“我喜欢的”歌单
        val favoriteSongs = dbHelper.getFavoriteSongs()
        if (favoriteSongs.isNotEmpty()) {
            val favObject = org.json.JSONObject()
            favObject.put("歌单名字", "我喜欢的")
            val songsArray = org.json.JSONArray()
            for (song in favoriteSongs) {
                val songObject = org.json.JSONObject()
                songObject.put("title", song.title)
                songObject.put("artist", song.artist)
                songObject.put("album", song.album)
                songObject.put("path", song.path)
                songObject.put("duration", song.duration)
                songObject.put("size", song.size)
                songObject.put("albumId", song.albumId)
                songsArray.put(songObject)
            }
            favObject.put("歌曲列表", songsArray)
            jsonArray.put(favObject)
        }

        // 2. 导出“遗忘的沙漏”歌单
        val blacklistedSongs = dbHelper.getBlacklistedSongs()
        if (blacklistedSongs.isNotEmpty()) {
            val blackObject = org.json.JSONObject()
            blackObject.put("歌单名字", "遗忘的沙漏")
            val songsArray = org.json.JSONArray()
            for (song in blacklistedSongs) {
                val songObject = org.json.JSONObject()
                songObject.put("title", song.title)
                songObject.put("artist", song.artist)
                songObject.put("album", song.album)
                songObject.put("path", song.path)
                songObject.put("duration", song.duration)
                songObject.put("size", song.size)
                songObject.put("albumId", song.albumId)
                songsArray.put(songObject)
            }
            blackObject.put("歌曲列表", songsArray)
            jsonArray.put(blackObject)
        }

        // 3. 导出自定义歌单
        val playlists = dbHelper.getPlaylists()
        for (playlist in playlists) {
            val songs = dbHelper.getSongsInPlaylist(playlist.id)
            val playlistObject = org.json.JSONObject()
            playlistObject.put("歌单名字", playlist.name)

            val songsArray = org.json.JSONArray()
            for (song in songs) {
                val songObject = org.json.JSONObject()
                songObject.put("title", song.title)
                songObject.put("artist", song.artist)
                songObject.put("album", song.album)
                songObject.put("path", song.path)
                songObject.put("duration", song.duration)
                songObject.put("size", song.size)
                songObject.put("albumId", song.albumId)
                songsArray.put(songObject)
            }
            playlistObject.put("歌曲列表", songsArray)
            jsonArray.put(playlistObject)
        }
        jsonArray.toString(2)
    }

    suspend fun importPlaylistsJson(jsonContent: String): ImportResult = withContext(Dispatchers.IO) {
        var playlistsImported = 0
        var successCount = 0
        var failureCount = 0
        val failedSongsList = mutableListOf<String>()

        try {
            val jsonArray = org.json.JSONArray(jsonContent)

            // 1. 获取本地数据库中已有的所有歌曲
            val allLocalSongs = dbHelper.getAllSongs(includeBlacklisted = true)
            // 建立 path -> Song 映射
            val pathMap = allLocalSongs.associateBy { it.path }
            // 建立 "title_artist" -> Song 映射
            val titleArtistMap = mutableMapOf<String, Song>()
            for (song in allLocalSongs) {
                val key = "${song.title.trim().lowercase()}_${song.artist.trim().lowercase()}"
                titleArtistMap[key] = song
            }

            // 获取当前已有的自定义歌单
            val currentPlaylists = dbHelper.getPlaylists()
            val playlistNameMap = currentPlaylists.associateBy { it.name }

            for (i in 0 until jsonArray.length()) {
                val playlistObj = jsonArray.getJSONObject(i)
                val playlistName = playlistObj.optString("歌单名字")
                if (playlistName.isNullOrBlank()) continue

                val isFavoriteList = playlistName == "我喜欢的"
                val isBlacklistList = playlistName == "遗忘的沙漏"

                var playlistId: Long? = null
                if (!isFavoriteList && !isBlacklistList) {
                    // 自定义歌单：查找或创建
                    playlistId = playlistNameMap[playlistName]?.id
                    if (playlistId == null) {
                        playlistId = dbHelper.createPlaylist(playlistName)
                        if (playlistId == -1L) {
                            playlistId = dbHelper.getPlaylistIdByName(playlistName)
                        }
                    }
                    if (playlistId == null || playlistId == -1L) {
                        continue
                    }
                }

                playlistsImported++

                val songsArray = playlistObj.optJSONArray("歌曲列表") ?: org.json.JSONArray()
                for (j in 0 until songsArray.length()) {
                    val songObj = songsArray.getJSONObject(j)
                    val path = songObj.optString("path")
                    val title = songObj.optString("title")
                    val artist = songObj.optString("artist")

                    // 尝试精准与模糊匹配
                    var matchedSong = pathMap[path]
                    if (matchedSong == null && !title.isNullOrEmpty() && !artist.isNullOrEmpty()) {
                        val key = "${title.trim().lowercase()}_${artist.trim().lowercase()}"
                        matchedSong = titleArtistMap[key]
                    }

                    if (matchedSong != null) {
                        when {
                            isFavoriteList -> {
                                dbHelper.setFavorite(matchedSong.id, true)
                                successCount++
                            }

                            isBlacklistList -> {
                                dbHelper.setBlacklisted(matchedSong.id, true)
                                successCount++
                            }

                            else -> {
                                val success = dbHelper.addSongToPlaylist(playlistId!!, matchedSong.id)
                                if (success) {
                                    successCount++
                                }
                            }
                        }
                    } else {
                        // 本地找不到已有歌曲匹配，导入原始数据并作为虚拟无源歌曲存入 TABLE_SONGS
                        val generatedId = System.currentTimeMillis() * 1000 + (j * 10 + i) + (1..9).random()
                        val songTitle = if (title.isNullOrBlank()) "未知歌曲" else title
                        val songArtist = if (artist.isNullOrBlank()) "未知歌手" else artist
                        val newSong = Song(
                            id = generatedId,
                            title = songTitle,
                            artist = songArtist,
                            album = songObj.optString("album", "未知专辑"),
                            path = if (path.isNullOrBlank()) "" else path,
                            duration = songObj.optLong("duration", 0L),
                            size = songObj.optLong("size", 0L),
                            albumId = songObj.optLong("albumId", 0L),
                            isFavorite = isFavoriteList,
                            isBlacklisted = isBlacklistList
                        )
                        val insertedId = dbHelper.insertSong(newSong)
                        if (insertedId != -1L) {
                            if (!isFavoriteList && !isBlacklistList && playlistId != null) {
                                dbHelper.addSongToPlaylist(playlistId, generatedId)
                            }
                        }
                        failureCount++
                        failedSongsList.add("$songTitle - $songArtist")
                    }
                }
            }

            // 重新加载歌曲和歌单列表以刷新 UI
            loadSongs()
            loadPlaylists()

            ImportResult(
                playlistsImported = playlistsImported,
                successCount = successCount,
                failureCount = failureCount,
                failedSongs = failedSongsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        PlaybackManager.stop()
    }
}

data class LibraryDisplayData(
    val displayList: List<Any> = emptyList(),
    val indexMap: Map<Char, Int> = emptyMap()
)

data class ArtistDisplayData(
    val displayList: List<Any> = emptyList(),
    val indexMap: Map<Char, Int> = emptyMap()
)
