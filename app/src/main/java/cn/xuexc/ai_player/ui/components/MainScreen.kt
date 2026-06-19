package cn.xuexc.ai_player.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.xuexc.ai_player.data.*
import cn.xuexc.ai_player.playback.PlayMode
import cn.xuexc.ai_player.ui.FolderNode
import cn.xuexc.ai_player.ui.ScanStatus
import cn.xuexc.ai_player.ui.SongViewModel
import cn.xuexc.ai_player.ui.SortOrder
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class DragDirection {
    VERTICAL,
    HORIZONTAL,
}

@Composable
fun MainScreen(viewModel: SongViewModel) {
    val context = LocalContext.current

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersionInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val currentVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.2.2"
        } catch (e: Exception) {
            "1.2.2"
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val curClean = current.trim().lowercase().removePrefix("v")
        val latClean = latest.trim().lowercase().removePrefix("v")
        if (curClean == latClean) return false

        val curParts = curClean.split(".")
        val latParts = latClean.split(".")
        val length = maxOf(curParts.size, latParts.size)
        for (i in 0 until length) {
            val curNum = curParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latNum = latParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (latNum > curNum) return true
            if (curNum > latNum) return false
        }
        return false
    }

    var currentScreen by rememberSaveable { mutableStateOf(Screen.Library) }
    val pagerState =
        rememberPagerState(
            initialPage =
                when (currentScreen) {
                    Screen.Library -> 0
                    Screen.Playlists -> 1
                    Screen.Artists -> 2
                },
            pageCount = { 3 },
        )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (!isProgrammaticScroll) {
            val targetScreen =
                when (pagerState.currentPage) {
                    0 -> Screen.Library
                    1 -> Screen.Playlists
                    2 -> Screen.Artists
                    else -> Screen.Library
                }
            if (currentScreen != targetScreen) {
                currentScreen = targetScreen
            }
        }
    }

    LaunchedEffect(currentScreen) {
        val targetPage =
            when (currentScreen) {
                Screen.Library -> 0
                Screen.Playlists -> 1
                Screen.Artists -> 2
            }
        if (pagerState.currentPage != targetPage) {
            try {
                isProgrammaticScroll = true
                pagerState.animateScrollToPage(targetPage)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }
    var activePlaylistId by rememberSaveable {
        mutableStateOf<Long?>(null)
    } // -1 for Favorites, -2 for Blacklist, positive for custom, null for none

    LaunchedEffect(activePlaylistId) { viewModel.activePlaylistIdForRefresh = activePlaylistId }
    var activePlaylistName by rememberSaveable { mutableStateOf("") }
    var activeArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var previousScreen by rememberSaveable { mutableStateOf<Screen?>(null) }
    var previousPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastActivePlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    if (activePlaylistId != null) {
        lastActivePlaylistId = activePlaylistId
    }
    var lastActiveArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    if (activeArtistName != null) {
        lastActiveArtistName = activeArtistName
    }
    val artistListState = rememberLazyListState()
    val songListState = rememberLazyListState()
    val playlistSongsListState = rememberLazyListState()
    val artistSongsDetailListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun checkForUpdates(showToastIfLatest: Boolean = true) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url =
                    URL(
                        "https://api.github.com/repos/X-X-X-X-X-X-X-X-X-X-X-X-X/AI-Player/releases/latest"
                    )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "AI-Player-App")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.getString("tag_name")
                    val htmlUrl = json.getString("html_url")
                    val body = json.optString("body", "")

                    if (isNewerVersion(currentVersionName, tagName)) {
                        withContext(Dispatchers.Main) {
                            latestVersionInfo = UpdateInfo(tagName, body, htmlUrl)
                            showUpdateDialog = true
                            isCheckingUpdate = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isCheckingUpdate = false
                            if (showToastIfLatest) {
                                Toast.makeText(
                                        context,
                                        "当前已是最新版本 (v$currentVersionName)",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        Toast.makeText(
                                context,
                                "检查更新失败，响应码: ${connection.responseCode}",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                }
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isCheckingUpdate = false
                    Toast.makeText(context, "网络连接失败，请检查网络", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isCheckingUpdate = false
                    Toast.makeText(context, "检查更新出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showFullPlayer by rememberSaveable { mutableStateOf(false) }

    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
    val playerOffsetY = remember { Animatable(screenHeight) }
    var hasOpenedPlayer by remember { mutableStateOf(false) }

    val horizontalOffset = remember { Animatable(0f) }
    var dragDirection by remember { mutableStateOf<DragDirection?>(null) }
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 70.dp.toPx() } }

    LaunchedEffect(showFullPlayer) {
        if (showFullPlayer) {
            hasOpenedPlayer = true
            playerOffsetY.animateTo(
                targetValue = 0f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )
        } else {
            launch {
                playerOffsetY.animateTo(
                    targetValue = screenHeight,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                )
            }
            if (hasOpenedPlayer) {
                viewModel.loadSongs()
                viewModel.loadPlaylists()
                if (activePlaylistId == -1L) {
                    viewModel.loadFavoriteSongs()
                } else if (activePlaylistId == -2L) {
                    viewModel.loadBlacklistSongs()
                } else if (activePlaylistId != null) {
                    viewModel.loadSongsInPlaylist(activePlaylistId!!)
                }
            }
        }
    }

    val miniPlayerDragModifier =
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragDirection = null },
                onDrag = { change, dragAmount ->
                    if (dragDirection == null) {
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        if (kotlin.math.abs(dx) > 10f || kotlin.math.abs(dy) > 10f) {
                            dragDirection =
                                if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                    DragDirection.HORIZONTAL
                                } else {
                                    DragDirection.VERTICAL
                                }
                        }
                    }

                    when (dragDirection) {
                        DragDirection.VERTICAL -> {
                            change.consume()
                            coroutineScope.launch {
                                playerOffsetY.snapTo(
                                    (playerOffsetY.value + dragAmount.y).coerceIn(0f, screenHeight)
                                )
                            }
                        }
                        DragDirection.HORIZONTAL -> {
                            change.consume()
                            coroutineScope.launch {
                                horizontalOffset.snapTo(horizontalOffset.value + dragAmount.x)
                            }
                        }
                        null -> {
                            change.consume()
                        }
                    }
                },
                onDragEnd = {
                    when (dragDirection) {
                        DragDirection.VERTICAL -> {
                            coroutineScope.launch {
                                if (playerOffsetY.value < screenHeight * 0.85f) {
                                    showFullPlayer = true
                                } else {
                                    playerOffsetY.animateTo(
                                        screenHeight,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                    )
                                }
                            }
                        }
                        DragDirection.HORIZONTAL -> {
                            coroutineScope.launch {
                                val offsetVal = horizontalOffset.value
                                val queue = viewModel.playbackQueue.value
                                val hasMultipleSongs = queue.size > 1
                                if (offsetVal > swipeThresholdPx && hasMultipleSongs) {
                                    // 右滑上一首
                                    viewModel.playPreviousSong(context)
                                    horizontalOffset.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessMedium),
                                    )
                                } else if (offsetVal < -swipeThresholdPx && hasMultipleSongs) {
                                    // 左滑下一首
                                    viewModel.playNextSong(context)
                                    horizontalOffset.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessMedium),
                                    )
                                } else {
                                    horizontalOffset.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    )
                                }
                            }
                        }
                        null -> {}
                    }
                    dragDirection = null
                },
                onDragCancel = {
                    coroutineScope.launch {
                        playerOffsetY.animateTo(
                            screenHeight,
                            spring(dampingRatio = Spring.DampingRatioNoBouncy),
                        )
                        horizontalOffset.animateTo(
                            0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        )
                    }
                    dragDirection = null
                },
            )
        }

    val playerDragModifier =
        Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        playerOffsetY.snapTo(
                            (playerOffsetY.value + dragAmount).coerceIn(0f, screenHeight)
                        )
                    }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        if (playerOffsetY.value > screenHeight * 0.15f) {
                            showFullPlayer = false
                        } else {
                            playerOffsetY.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy),
                            )
                        }
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        if (showFullPlayer) {
                            playerOffsetY.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy),
                            )
                        } else {
                            playerOffsetY.animateTo(
                                screenHeight,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy),
                            )
                        }
                    }
                },
            )
        }

    val exportPlaylistsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
            onResult = { uri ->
                uri?.let {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val jsonString = viewModel.exportPlaylistsJson()
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                            }
                            val displayPath =
                                try {
                                    var fileName: String? = null
                                    if (it.scheme == "content") {
                                        context.contentResolver
                                            .query(
                                                it,
                                                arrayOf(
                                                    android.provider.OpenableColumns.DISPLAY_NAME
                                                ),
                                                null,
                                                null,
                                                null,
                                            )
                                            ?.use { cursor ->
                                                if (cursor.moveToFirst()) {
                                                    val idx =
                                                        cursor.getColumnIndex(
                                                            android.provider.OpenableColumns
                                                                .DISPLAY_NAME
                                                        )
                                                    if (idx != -1) {
                                                        fileName = cursor.getString(idx)
                                                    }
                                                }
                                            }
                                    }

                                    val rawPath = it.path
                                    if (rawPath != null && rawPath.contains(":")) {
                                        val decoded = java.net.URLDecoder.decode(rawPath, "UTF-8")
                                        decoded.substringAfter(":")
                                    } else {
                                        fileName ?: rawPath ?: "未知文件"
                                    }
                                } catch (e: Exception) {
                                    it.lastPathSegment ?: "未知文件"
                                }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                        context,
                                        "歌单导出成功，已保存至: $displayPath",
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            },
        )

    var importResult by remember { mutableStateOf<cn.xuexc.ai_player.ui.ImportResult?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val importPlaylistsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                uri?.let {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val content =
                                context.contentResolver.openInputStream(it)?.use { inputStream ->
                                    inputStream.bufferedReader().use { reader -> reader.readText() }
                                }
                            if (content != null) {
                                val result = viewModel.importPlaylistsJson(content)
                                withContext(Dispatchers.Main) { importResult = result }
                            } else {
                                withContext(Dispatchers.Main) { importError = "读取文件失败" }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) { importError = "导入失败: ${e.message}" }
                        }
                    }
                }
            },
        )

    // Multi-selection states
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedSongs = remember { mutableStateListOf<Song>() }
    var songsToDelete by remember { mutableStateOf<List<Song>?>(null) }
    var songsToBlacklist by remember { mutableStateOf<List<Song>?>(null) }
    var showBatchAddToPlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(
        enabled =
            isMultiSelectMode ||
                showFullPlayer ||
                activeArtistName != null ||
                activePlaylistId != null
    ) {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedSongs.clear()
        } else if (showFullPlayer) {
            showFullPlayer = false
        } else if (activeArtistName != null) {
            if (previousScreen != null) {
                currentScreen = previousScreen!!
                activePlaylistId = previousPlaylistId
                previousScreen = null
                previousPlaylistId = null
            }
            activeArtistName = null
        } else if (activePlaylistId != null) {
            activePlaylistId = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activePlaylistId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkSongsPhysicalModified(context)
                viewModel.startScan(context, isSilent = true)
                if (activePlaylistId == -1L) {
                    viewModel.loadFavoriteSongs()
                } else if (activePlaylistId == -2L) {
                    viewModel.loadBlacklistSongs()
                } else if (activePlaylistId != null) {
                    viewModel.loadSongsInPlaylist(activePlaylistId!!)
                }
                cn.xuexc.ai_player.playback.PlaybackManager.checkAndUpdateCurrentSongMetadata(
                    context
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeListState by
        remember(currentScreen, activePlaylistId, activeArtistName) {
            derivedStateOf {
                when {
                    activePlaylistId != null -> playlistSongsListState
                    activeArtistName != null -> artistSongsDetailListState
                    currentScreen == Screen.Library -> songListState
                    currentScreen == Screen.Artists -> artistListState
                    else -> null
                }
            }
        }

    val showBackToTop by remember {
        derivedStateOf {
            val state = activeListState
            state != null && state.firstVisibleItemIndex > 3
        }
    }

    val isAtBottom by
        remember(activeListState) {
            derivedStateOf {
                val state = activeListState ?: return@derivedStateOf false
                val layoutInfo = state.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isEmpty()) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    lastVisibleItem.index == layoutInfo.totalItemsCount - 1 &&
                        (lastVisibleItem.offset + lastVisibleItem.size) <=
                            (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 10)
                }
            }
        }
    var showBlockedFoldersDialog by remember { mutableStateOf(false) }
    var showSortOrderDialog by remember { mutableStateOf(false) }
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isSortAscending by viewModel.isSortAscending.collectAsState()
    val isTitleSort = sortOrder == SortOrder.BY_TITLE
    val blockedFolders by viewModel.blockedFolders.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()

    val themeName by viewModel.themeName.collectAsState()
    val currentAccent =
        remember(themeName) {
            AccentColor.values().firstOrNull { it.id == themeName } ?: AccentColor.TitaniumGray
        }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appColors = remember(currentAccent, isDarkMode) { getAppColors(currentAccent, isDarkMode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !isDarkMode
        }
    }

    val songs by viewModel.songs.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val libraryDisplayData by viewModel.libraryDisplayData.collectAsState()
    val songsDisplayList = libraryDisplayData.displayList
    val songsIndexMap = libraryDisplayData.indexMap

    val artistDisplayData by viewModel.artistDisplayData.collectAsState()
    val artistDisplayList = artistDisplayData.displayList
    val artistIndexMap = artistDisplayData.indexMap

    // Playback state
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()

    LaunchedEffect(currentSong) {
        if (currentSong == null) {
            showFullPlayer = false
        }
    }

    // Playlists state
    val playlists by viewModel.playlists.collectAsState()
    val isSongsLoaded by viewModel.isSongsLoaded.collectAsState()
    val isPlaylistsLoaded by viewModel.isPlaylistsLoaded.collectAsState()
    val favoriteSongsCount by viewModel.favoriteSongsCount.collectAsState()
    val blacklistSongsCount by viewModel.blacklistSongsCount.collectAsState()
    val playlistSongs by viewModel.currentPlaylistSongs.collectAsState()

    val showLocateSong by
        remember(
            currentSong,
            currentScreen,
            activePlaylistId,
            activeArtistName,
            songs,
            playlistSongs,
        ) {
            derivedStateOf {
                val curSong = currentSong ?: return@derivedStateOf false
                when {
                    activePlaylistId != null -> playlistSongs.any { it.id == curSong.id }
                    activeArtistName != null ->
                        songs.any { it.artist == activeArtistName && it.id == curSong.id }
                    currentScreen == Screen.Library -> songs.any { it.id == curSong.id }
                    else -> false
                }
            }
        }

    // Dialog flags
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var songToRemoveFromPlaylist by remember { mutableStateOf<Pair<Long, Song>?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val listBottomPadding by
        animateDpAsState(
            targetValue =
                if (isMultiSelectMode && selectedSongs.isNotEmpty()) 100.dp
                else if (currentSong != null) 100.dp else 16.dp,
            label = "list_bottom_padding",
        )

    LaunchedEffect(currentScreen, activePlaylistId, activeArtistName) {
        isMultiSelectMode = false
        selectedSongs.clear()
    }

    // Determine correct permission based on Android version
    val requiredPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted ->
            if (isGranted) {
                viewModel.startScan(context)
            } else {
                Toast.makeText(context, "需要存储权限以扫描本地歌曲", Toast.LENGTH_LONG).show()
            }
        }

    fun checkAndStartScan() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, requiredPermission)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.startScan(context)
        } else {
            permissionLauncher.launch(requiredPermission)
        }
    }

    val locateCurrentSong = {
        val curSong = currentSong
        if (curSong != null) {
            coroutineScope.launch {
                when {
                    activePlaylistId != null -> {
                        val index = playlistSongs.indexOfFirst { it.id == curSong.id }
                        if (index != -1) {
                            playlistSongsListState.scrollToItem(index)
                        }
                    }

                    activeArtistName != null -> {
                        val artistSongs = songs.filter { it.artist == activeArtistName }
                        val index = artistSongs.indexOfFirst { it.id == curSong.id }
                        if (index != -1) {
                            artistSongsDetailListState.scrollToItem(index)
                        }
                    }

                    currentScreen == Screen.Library -> {
                        if (isTitleSort) {
                            val index =
                                songsDisplayList.indexOfFirst {
                                    it is SongItemWithLetter && it.song.id == curSong.id
                                }
                            if (index != -1) {
                                songListState.scrollToItem(index)
                            }
                        } else {
                            val index = songs.indexOfFirst { it.id == curSong.id }
                            if (index != -1) {
                                songListState.scrollToItem(index)
                            }
                        }
                    }
                }
            }
        }
    }

    var isSearching by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .background(appColors.mainBackground)
                        .padding(innerPadding)
            ) {
                TopBar(
                    activePlaylistId = activePlaylistId,
                    activePlaylistName = activePlaylistName,
                    onBackToPlaylists = { activePlaylistId = null },
                    activeArtistName = activeArtistName,
                    onBackToArtists = {
                        if (previousScreen != null) {
                            currentScreen = previousScreen!!
                            activePlaylistId = previousPlaylistId
                            previousScreen = null
                            previousPlaylistId = null
                        }
                        activeArtistName = null
                    },
                    currentScreen = currentScreen,
                    onScreenChange = {
                        currentScreen = it
                        activePlaylistId = null
                        activeArtistName = null
                        previousScreen = null
                        previousPlaylistId = null
                    },
                    isSearching = isSearching,
                    onSearchStateChange = { isSearching = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    showThemeDialog = { showThemeDialog = true },
                    showSleepTimerDialog = { showSleepTimerDialog = true },
                    onScanClick = { checkAndStartScan() },
                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                    onBlockedFoldersClick = { showBlockedFoldersDialog = true },
                    onSortOrderClick = { showSortOrderDialog = true },
                    onImportPlaylistsClick = {
                        importPlaylistsLauncher.launch(arrayOf("application/json"))
                    },
                    onExportPlaylistsClick = {
                        exportPlaylistsLauncher.launch("ai_playlists.json")
                    },
                    appColors = appColors,
                    currentAccent = currentAccent,
                    isDark = isDarkMode,
                    scanState = scanState,
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdateClick = { checkForUpdates(true) },
                )

                Box(modifier = Modifier.fillMaxWidth().weight(1.0f)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activePlaylistId != null,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        val currentPlaylistId = lastActivePlaylistId
                        if (currentPlaylistId != null) {
                            // PLAYLIST DETAILS PAGE
                            Column(modifier = Modifier.fillMaxSize()) {
                                // 随机播放控制栏 / 多选控制栏
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(
                                                start = 8.dp,
                                                end = 16.dp,
                                                top = 2.dp,
                                                bottom = 2.dp,
                                            ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    if (isMultiSelectMode) {
                                        val isAllSelected = selectedSongs.size == playlistSongs.size
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 0.dp),
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier.clip(RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            if (isAllSelected) {
                                                                selectedSongs.clear()
                                                            } else {
                                                                selectedSongs.clear()
                                                                selectedSongs.addAll(playlistSongs)
                                                            }
                                                        }
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp,
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "全选",
                                                    tint =
                                                        if (isAllSelected) currentAccent.mainColor
                                                        else
                                                            appColors.textColorSecondary.copy(
                                                                alpha = 0.4f
                                                            ),
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAllSelected) "取消全选" else "全选",
                                                    color =
                                                        if (isAllSelected) currentAccent.mainColor
                                                        else appColors.textColorSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "已选择 ${selectedSongs.size} 首歌曲",
                                                color = appColors.textColorPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "退出多选",
                                                tint = appColors.textColorSecondary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier =
                                                Modifier.clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        val shuffled = playlistSongs.shuffled()
                                                        if (shuffled.isNotEmpty()) {
                                                            viewModel.setPlaybackQueue(shuffled)
                                                            while (
                                                                viewModel.playMode.value !=
                                                                    PlayMode.Shuffle
                                                            ) {
                                                                viewModel.togglePlayMode()
                                                            }
                                                            viewModel.playSong(context, shuffled[0])
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shuffle,
                                                contentDescription = "随机播放",
                                                tint = currentAccent.mainColor,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "随机播放",
                                                color = currentAccent.mainColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "共 ${playlistSongs.size} 首歌曲",
                                                color = appColors.textColorSecondary,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }

                                if (playlistSongs.isEmpty()) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .weight(1.0f)
                                                .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text =
                                                if (currentPlaylistId == -2L) "遗忘的沙漏为空" else "歌单为空",
                                            color = appColors.textColorSecondary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = playlistSongsListState,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .weight(1.0f)
                                                .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = listBottomPadding),
                                    ) {
                                        items(playlistSongs, key = { it.id }) { song ->
                                            val isCurrent = currentSong?.id == song.id
                                            SongItemCard(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && isPlaying,
                                                onDelete = { songToDelete = song },
                                                appColors = appColors,
                                                onPlayNextClick = {
                                                    viewModel.insertToQueueAsNext(song)
                                                },
                                                onNavigateToArtist = { artist ->
                                                    previousScreen = currentScreen
                                                    previousPlaylistId = currentPlaylistId
                                                    currentScreen = Screen.Artists
                                                    activeArtistName = artist
                                                    activePlaylistId = null
                                                },
                                                onFavoriteToggle =
                                                    if (currentPlaylistId == -2L) null
                                                    else {
                                                        { viewModel.toggleFavorite(song) }
                                                    },
                                                onBlacklistToggle =
                                                    if (currentPlaylistId == -2L) null
                                                    else {
                                                        {
                                                            viewModel.addToBlacklist(context, song)
                                                            if (currentPlaylistId == -1L) {
                                                                viewModel.loadFavoriteSongs()
                                                            } else {
                                                                viewModel.loadSongsInPlaylist(
                                                                    currentPlaylistId
                                                                )
                                                            }
                                                        }
                                                    },
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        val selected =
                                                            selectedSongs.any { it.id == song.id }
                                                        if (selected)
                                                            selectedSongs.removeAll {
                                                                it.id == song.id
                                                            }
                                                        else selectedSongs.add(song)
                                                    } else {
                                                        viewModel.setPlaybackQueue(playlistSongs)
                                                        viewModel.playSong(context, song)
                                                    }
                                                },
                                                customActionIcon =
                                                    when {
                                                        isMultiSelectMode -> null
                                                        currentPlaylistId == -2L ->
                                                            Icons.Default
                                                                .Refresh // Refresh icon to restore
                                                        // from blacklist
                                                        currentPlaylistId > 0L ->
                                                            Icons.Default
                                                                .Clear // Cross to remove from
                                                        // playlist
                                                        else -> null
                                                    },
                                                onCustomAction =
                                                    when {
                                                        currentPlaylistId == -2L -> {
                                                            {
                                                                viewModel.removeFromBlacklist(song)
                                                                Toast.makeText(
                                                                        context,
                                                                        "已从遗忘的沙漏恢复",
                                                                        Toast.LENGTH_SHORT,
                                                                    )
                                                                    .show()
                                                            }
                                                        }

                                                        currentPlaylistId > 0L -> {
                                                            {
                                                                songToRemoveFromPlaylist =
                                                                    currentPlaylistId to song
                                                            }
                                                        }

                                                        else -> null
                                                    },
                                                inSelectionMode = isMultiSelectMode,
                                                isSelected = selectedSongs.any { it.id == song.id },
                                                onSelectionChange = { selected ->
                                                    if (selected) {
                                                        selectedSongs.add(song)
                                                    } else {
                                                        selectedSongs.removeAll { it.id == song.id }
                                                    }
                                                },
                                                onLongClick = {
                                                    isMultiSelectMode = true
                                                    selectedSongs.clear()
                                                    selectedSongs.add(song)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeArtistName != null,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        val currentArtistName = lastActiveArtistName
                        if (currentArtistName != null) {
                            // ARTIST SONGS DETAILS PAGE
                            val artistSongs =
                                remember(songs, currentArtistName) {
                                    songs.filter { it.artist == currentArtistName }
                                }
                            Column(modifier = Modifier.fillMaxSize()) {
                                // 随机播放控制栏 / 多选控制栏
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(
                                                start = 8.dp,
                                                end = 16.dp,
                                                top = 2.dp,
                                                bottom = 2.dp,
                                            ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    if (isMultiSelectMode) {
                                        val isAllSelected = selectedSongs.size == artistSongs.size
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 0.dp),
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier.clip(RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            if (isAllSelected) {
                                                                selectedSongs.clear()
                                                            } else {
                                                                selectedSongs.clear()
                                                                selectedSongs.addAll(artistSongs)
                                                            }
                                                        }
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp,
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "全选",
                                                    tint =
                                                        if (isAllSelected) currentAccent.mainColor
                                                        else
                                                            appColors.textColorSecondary.copy(
                                                                alpha = 0.4f
                                                            ),
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAllSelected) "取消全选" else "全选",
                                                    color =
                                                        if (isAllSelected) currentAccent.mainColor
                                                        else appColors.textColorSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "已选择 ${selectedSongs.size} 首歌曲",
                                                color = appColors.textColorPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "退出多选",
                                                tint = appColors.textColorSecondary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier =
                                                Modifier.clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        val shuffled = artistSongs.shuffled()
                                                        if (shuffled.isNotEmpty()) {
                                                            viewModel.setPlaybackQueue(shuffled)
                                                            while (
                                                                viewModel.playMode.value !=
                                                                    PlayMode.Shuffle
                                                            ) {
                                                                viewModel.togglePlayMode()
                                                            }
                                                            viewModel.playSong(context, shuffled[0])
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shuffle,
                                                contentDescription = "随机播放",
                                                tint = currentAccent.mainColor,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "随机播放",
                                                color = currentAccent.mainColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "共 ${artistSongs.size} 首歌曲",
                                                color = appColors.textColorSecondary,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }

                                if (artistSongs.isEmpty()) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .weight(1.0f)
                                                .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "暂无该歌手的歌曲",
                                            color = appColors.textColorSecondary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = artistSongsDetailListState,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .weight(1.0f)
                                                .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = listBottomPadding),
                                    ) {
                                        items(artistSongs, key = { it.id }) { song ->
                                            val isCurrent = currentSong?.id == song.id
                                            SongItemCard(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && isPlaying,
                                                onDelete = { songToDelete = song },
                                                appColors = appColors,
                                                onPlayNextClick = {
                                                    viewModel.insertToQueueAsNext(song)
                                                },
                                                onNavigateToArtist = { artist ->
                                                    previousScreen = currentScreen
                                                    previousPlaylistId = activePlaylistId
                                                    currentScreen = Screen.Artists
                                                    activeArtistName = artist
                                                    activePlaylistId = null
                                                },
                                                onFavoriteToggle = {
                                                    viewModel.toggleFavorite(song)
                                                },
                                                onBlacklistToggle = {
                                                    viewModel.addToBlacklist(context, song)
                                                },
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        val selected =
                                                            selectedSongs.any { it.id == song.id }
                                                        if (selected)
                                                            selectedSongs.removeAll {
                                                                it.id == song.id
                                                            }
                                                        else selectedSongs.add(song)
                                                    } else {
                                                        viewModel.setPlaybackQueue(artistSongs)
                                                        viewModel.playSong(context, song)
                                                    }
                                                },
                                                inSelectionMode = isMultiSelectMode,
                                                isSelected = selectedSongs.any { it.id == song.id },
                                                onSelectionChange = { selected ->
                                                    if (selected) {
                                                        selectedSongs.add(song)
                                                    } else {
                                                        selectedSongs.removeAll { it.id == song.id }
                                                    }
                                                },
                                                onLongClick = {
                                                    isMultiSelectMode = true
                                                    selectedSongs.clear()
                                                    selectedSongs.add(song)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activePlaylistId == null && activeArtistName == null) {
                        // TAB PAGES
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
                            page ->
                            val screen =
                                when (page) {
                                    0 -> Screen.Library
                                    1 -> Screen.Playlists
                                    2 -> Screen.Artists
                                    else -> Screen.Library
                                }
                            when (screen) {
                                Screen.Library -> {
                                    // LIBRARY TAB

                                    var selectedLetter by remember { mutableStateOf<Char?>(null) }
                                    var showLetterIndicator by remember { mutableStateOf(false) }

                                    val firstVisibleIndex by remember {
                                        derivedStateOf { songListState.firstVisibleItemIndex }
                                    }
                                    LaunchedEffect(firstVisibleIndex, isTitleSort) {
                                        if (
                                            isTitleSort &&
                                                !showLetterIndicator &&
                                                songsDisplayList.isNotEmpty()
                                        ) {
                                            val visibleItem =
                                                songsDisplayList.getOrNull(firstVisibleIndex)
                                            val currentLetter =
                                                when (visibleItem) {
                                                    is Char -> visibleItem
                                                    is SongItemWithLetter -> visibleItem.letter
                                                    else -> null
                                                }
                                            if (currentLetter != null) {
                                                selectedLetter = currentLetter
                                            }
                                        }
                                    }

                                    val scrollToLetter = { letter: Char ->
                                        val targetIndex = getTargetIndex(letter, songsIndexMap)
                                        if (targetIndex != -1) {
                                            coroutineScope.launch {
                                                songListState.scrollToItem(targetIndex)
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            LaunchedEffect(scanState) {
                                                val currentState = scanState
                                                if (currentState is ScanStatus.Success) {
                                                    Toast.makeText(
                                                            context,
                                                            "扫描完成，共发现 ${currentState.count} 首歌曲",
                                                            Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                    viewModel.resetScanState()
                                                } else if (currentState is ScanStatus.Error) {
                                                    Toast.makeText(
                                                            context,
                                                            "扫描出错: ${currentState.message}",
                                                            Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                    viewModel.resetScanState()
                                                }
                                            }

                                            // Songs list or Empty state
                                            if (songs.isEmpty()) {
                                                if (isSongsLoaded) {
                                                    Box(
                                                        modifier =
                                                            Modifier.fillMaxWidth()
                                                                .weight(1.0f)
                                                                .padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Column(
                                                            horizontalAlignment =
                                                                Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                        ) {
                                                            Box(
                                                                modifier =
                                                                    Modifier.size(80.dp)
                                                                        .clip(
                                                                            RoundedCornerShape(
                                                                                40.dp
                                                                            )
                                                                        )
                                                                        .background(
                                                                            appColors
                                                                                .textfieldContainer
                                                                        ),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default.PlayArrow,
                                                                    contentDescription = "No Music",
                                                                    tint =
                                                                        appColors.textColorSecondary
                                                                            .copy(alpha = 0.5f),
                                                                    modifier = Modifier.size(40.dp),
                                                                )
                                                            }
                                                            Spacer(
                                                                modifier = Modifier.height(16.dp)
                                                            )
                                                            Text(
                                                                text = "未扫描到本地歌曲",
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = appColors.textColorPrimary,
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "点击上方刷新按钮或下方按钮开始扫描",
                                                                fontSize = 12.sp,
                                                                color = appColors.textColorSecondary,
                                                            )
                                                            Spacer(
                                                                modifier = Modifier.height(24.dp)
                                                            )
                                                            Button(
                                                                onClick = { checkAndStartScan() },
                                                                colors =
                                                                    ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                            currentAccent.mainColor
                                                                    ),
                                                                shape = RoundedCornerShape(24.dp),
                                                                modifier =
                                                                    Modifier.width(160.dp)
                                                                        .height(48.dp),
                                                            ) {
                                                                Text(
                                                                    "扫描歌曲",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier =
                                                            Modifier.fillMaxWidth().weight(1.0f)
                                                    )
                                                }
                                            } else {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                ) {
                                                    // 随机播放控制栏
                                                    Row(
                                                        modifier =
                                                            Modifier.fillMaxWidth()
                                                                .padding(
                                                                    start = 8.dp,
                                                                    end = 16.dp,
                                                                    top = 2.dp,
                                                                    bottom = 2.dp,
                                                                ),
                                                        verticalAlignment =
                                                            Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                            Arrangement.SpaceBetween,
                                                    ) {
                                                        if (isMultiSelectMode) {
                                                            val isAllSelected =
                                                                selectedSongs.size == songs.size
                                                            Row(
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically,
                                                                modifier =
                                                                    Modifier.padding(start = 0.dp),
                                                            ) {
                                                                Row(
                                                                    modifier =
                                                                        Modifier.clip(
                                                                                RoundedCornerShape(
                                                                                    16.dp
                                                                                )
                                                                            )
                                                                            .clickable {
                                                                                if (isAllSelected) {
                                                                                    selectedSongs
                                                                                        .clear()
                                                                                } else {
                                                                                    selectedSongs
                                                                                        .clear()
                                                                                    selectedSongs
                                                                                        .addAll(
                                                                                            songs
                                                                                        )
                                                                                }
                                                                            }
                                                                            .padding(
                                                                                horizontal = 8.dp,
                                                                                vertical = 4.dp,
                                                                            ),
                                                                    verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                ) {
                                                                    Icon(
                                                                        imageVector =
                                                                            Icons.Default
                                                                                .CheckCircle,
                                                                        contentDescription = "全选",
                                                                        tint =
                                                                            if (isAllSelected)
                                                                                currentAccent
                                                                                    .mainColor
                                                                            else
                                                                                appColors
                                                                                    .textColorSecondary
                                                                                    .copy(
                                                                                        alpha = 0.4f
                                                                                    ),
                                                                        modifier =
                                                                            Modifier.size(13.dp),
                                                                    )
                                                                    Spacer(
                                                                        modifier =
                                                                            Modifier.width(4.dp)
                                                                    )
                                                                    Text(
                                                                        text =
                                                                            if (isAllSelected)
                                                                                "取消全选"
                                                                            else "全选",
                                                                        color =
                                                                            if (isAllSelected)
                                                                                currentAccent
                                                                                    .mainColor
                                                                            else
                                                                                appColors
                                                                                    .textColorSecondary,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                    )
                                                                }
                                                                Spacer(
                                                                    modifier = Modifier.width(8.dp)
                                                                )
                                                                Text(
                                                                    text =
                                                                        "已选择 ${selectedSongs.size} 首歌曲",
                                                                    color =
                                                                        appColors.textColorPrimary,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    isMultiSelectMode = false
                                                                    selectedSongs.clear()
                                                                },
                                                                modifier = Modifier.size(24.dp),
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default.Close,
                                                                    contentDescription = "退出多选",
                                                                    tint =
                                                                        appColors
                                                                            .textColorSecondary,
                                                                    modifier = Modifier.size(16.dp),
                                                                )
                                                            }
                                                        } else {
                                                            Row(
                                                                modifier =
                                                                    Modifier.clip(
                                                                            RoundedCornerShape(
                                                                                16.dp
                                                                            )
                                                                        )
                                                                        .clickable {
                                                                            val shuffled =
                                                                                songs.shuffled()
                                                                            if (
                                                                                shuffled
                                                                                    .isNotEmpty()
                                                                            ) {
                                                                                viewModel
                                                                                    .setPlaybackQueue(
                                                                                        shuffled
                                                                                    )
                                                                                while (
                                                                                    viewModel
                                                                                        .playMode
                                                                                        .value !=
                                                                                        PlayMode
                                                                                            .Shuffle
                                                                                ) {
                                                                                    viewModel
                                                                                        .togglePlayMode()
                                                                                }
                                                                                viewModel.playSong(
                                                                                    context,
                                                                                    shuffled[0],
                                                                                )
                                                                            }
                                                                        }
                                                                        .padding(
                                                                            horizontal = 8.dp,
                                                                            vertical = 4.dp,
                                                                        ),
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically,
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default.Shuffle,
                                                                    contentDescription = "随机播放",
                                                                    tint = currentAccent.mainColor,
                                                                    modifier = Modifier.size(13.dp),
                                                                )
                                                                Spacer(
                                                                    modifier = Modifier.width(4.dp)
                                                                )
                                                                Text(
                                                                    text = "随机播放",
                                                                    color = currentAccent.mainColor,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                )
                                                            }

                                                            Row(
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "共 ${songs.size} 首歌曲",
                                                                    color =
                                                                        appColors
                                                                            .textColorSecondary,
                                                                    fontSize = 11.sp,
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier =
                                                            Modifier.fillMaxWidth().weight(1.0f)
                                                    ) {
                                                        LazyColumn(
                                                            state = songListState,
                                                            modifier =
                                                                Modifier.fillMaxSize()
                                                                    .padding(
                                                                        start = 16.dp,
                                                                        end =
                                                                            if (isTitleSort) 36.dp
                                                                            else 16.dp,
                                                                    ),
                                                            verticalArrangement =
                                                                Arrangement.spacedBy(8.dp),
                                                            contentPadding =
                                                                PaddingValues(
                                                                    bottom = listBottomPadding
                                                                ),
                                                        ) {
                                                            if (isTitleSort) {
                                                                itemsIndexed(songsDisplayList) {
                                                                    index,
                                                                    item ->
                                                                    when (item) {
                                                                        is Char -> {
                                                                            CharacterHeader(
                                                                                letter = item,
                                                                                appColors =
                                                                                    appColors,
                                                                            )
                                                                        }

                                                                        is SongItemWithLetter -> {
                                                                            val song = item.song
                                                                            val isCurrent =
                                                                                currentSong?.id ==
                                                                                    song.id
                                                                            SongItemCard(
                                                                                song = song,
                                                                                isCurrent =
                                                                                    isCurrent,
                                                                                isPlaying =
                                                                                    isCurrent &&
                                                                                        isPlaying,
                                                                                onDelete = {
                                                                                    songToDelete =
                                                                                        song
                                                                                },
                                                                                appColors =
                                                                                    appColors,
                                                                                onPlayNextClick = {
                                                                                    viewModel
                                                                                        .insertToQueueAsNext(
                                                                                            song
                                                                                        )
                                                                                },
                                                                                onNavigateToArtist = {
                                                                                    artist ->
                                                                                    previousScreen =
                                                                                        currentScreen
                                                                                    previousPlaylistId =
                                                                                        activePlaylistId
                                                                                    currentScreen =
                                                                                        Screen
                                                                                            .Artists
                                                                                    activeArtistName =
                                                                                        artist
                                                                                    activePlaylistId =
                                                                                        null
                                                                                },
                                                                                onFavoriteToggle = {
                                                                                    viewModel
                                                                                        .toggleFavorite(
                                                                                            song
                                                                                        )
                                                                                },
                                                                                onBlacklistToggle = {
                                                                                    viewModel
                                                                                        .addToBlacklist(
                                                                                            context,
                                                                                            song,
                                                                                        )
                                                                                },
                                                                                onClick = {
                                                                                    if (
                                                                                        isMultiSelectMode
                                                                                    ) {
                                                                                        val selected =
                                                                                            selectedSongs
                                                                                                .any {
                                                                                                    it
                                                                                                        .id ==
                                                                                                        song
                                                                                                            .id
                                                                                                }
                                                                                        if (
                                                                                            selected
                                                                                        )
                                                                                            selectedSongs
                                                                                                .removeAll {
                                                                                                    it
                                                                                                        .id ==
                                                                                                        song
                                                                                                            .id
                                                                                                }
                                                                                        else
                                                                                            selectedSongs
                                                                                                .add(
                                                                                                    song
                                                                                                )
                                                                                    } else {
                                                                                        viewModel
                                                                                            .setPlaybackQueue(
                                                                                                songs
                                                                                            )
                                                                                        viewModel
                                                                                            .playSong(
                                                                                                context,
                                                                                                song,
                                                                                            )
                                                                                    }
                                                                                },
                                                                                customActionIcon =
                                                                                    Icons.Default
                                                                                        .Add,
                                                                                onCustomAction = {
                                                                                    selectedSongForAddToPlaylist =
                                                                                        song
                                                                                    showAddToPlaylistDialog =
                                                                                        true
                                                                                },
                                                                                inSelectionMode =
                                                                                    isMultiSelectMode,
                                                                                isSelected =
                                                                                    selectedSongs
                                                                                        .any {
                                                                                            it.id ==
                                                                                                song
                                                                                                    .id
                                                                                        },
                                                                                onSelectionChange = {
                                                                                    selected ->
                                                                                    if (selected) {
                                                                                        selectedSongs
                                                                                            .add(
                                                                                                song
                                                                                            )
                                                                                    } else {
                                                                                        selectedSongs
                                                                                            .removeAll {
                                                                                                it
                                                                                                    .id ==
                                                                                                    song
                                                                                                        .id
                                                                                            }
                                                                                    }
                                                                                },
                                                                                onLongClick = {
                                                                                    isMultiSelectMode =
                                                                                        true
                                                                                    selectedSongs
                                                                                        .clear()
                                                                                    selectedSongs
                                                                                        .add(song)
                                                                                },
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                items(songs, key = { it.id }) { song
                                                                    ->
                                                                    val isCurrent =
                                                                        currentSong?.id == song.id
                                                                    SongItemCard(
                                                                        song = song,
                                                                        isCurrent = isCurrent,
                                                                        isPlaying =
                                                                            isCurrent && isPlaying,
                                                                        onDelete = {
                                                                            songToDelete = song
                                                                        },
                                                                        appColors = appColors,
                                                                        onPlayNextClick = {
                                                                            viewModel
                                                                                .insertToQueueAsNext(
                                                                                    song
                                                                                )
                                                                        },
                                                                        onNavigateToArtist = {
                                                                            artist ->
                                                                            previousScreen =
                                                                                currentScreen
                                                                            previousPlaylistId =
                                                                                activePlaylistId
                                                                            currentScreen =
                                                                                Screen.Artists
                                                                            activeArtistName =
                                                                                artist
                                                                            activePlaylistId = null
                                                                        },
                                                                        onFavoriteToggle = {
                                                                            viewModel
                                                                                .toggleFavorite(
                                                                                    song
                                                                                )
                                                                        },
                                                                        onBlacklistToggle = {
                                                                            viewModel
                                                                                .addToBlacklist(
                                                                                    context,
                                                                                    song,
                                                                                )
                                                                        },
                                                                        onClick = {
                                                                            if (isMultiSelectMode) {
                                                                                val selected =
                                                                                    selectedSongs
                                                                                        .any {
                                                                                            it.id ==
                                                                                                song
                                                                                                    .id
                                                                                        }
                                                                                if (selected)
                                                                                    selectedSongs
                                                                                        .removeAll {
                                                                                            it.id ==
                                                                                                song
                                                                                                    .id
                                                                                        }
                                                                                else
                                                                                    selectedSongs
                                                                                        .add(song)
                                                                            } else {
                                                                                viewModel
                                                                                    .setPlaybackQueue(
                                                                                        songs
                                                                                    )
                                                                                viewModel.playSong(
                                                                                    context,
                                                                                    song,
                                                                                )
                                                                            }
                                                                        },
                                                                        customActionIcon =
                                                                            Icons.Default.Add,
                                                                        onCustomAction = {
                                                                            selectedSongForAddToPlaylist =
                                                                                song
                                                                            showAddToPlaylistDialog =
                                                                                true
                                                                        },
                                                                        inSelectionMode =
                                                                            isMultiSelectMode,
                                                                        isSelected =
                                                                            selectedSongs.any {
                                                                                it.id == song.id
                                                                            },
                                                                        onSelectionChange = {
                                                                            selected ->
                                                                            if (selected) {
                                                                                selectedSongs.add(
                                                                                    song
                                                                                )
                                                                            } else {
                                                                                selectedSongs
                                                                                    .removeAll {
                                                                                        it.id ==
                                                                                            song.id
                                                                                    }
                                                                            }
                                                                        },
                                                                        onLongClick = {
                                                                            isMultiSelectMode = true
                                                                            selectedSongs.clear()
                                                                            selectedSongs.add(song)
                                                                        },
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // 右侧字母定位滑块
                                                        if (
                                                            isTitleSort &&
                                                                songsDisplayList.isNotEmpty()
                                                        ) {
                                                            AlphabetIndexer(
                                                                modifier =
                                                                    Modifier.align(
                                                                            Alignment.CenterEnd
                                                                        )
                                                                        .width(16.dp)
                                                                        .padding(
                                                                            end = 4.dp,
                                                                            bottom =
                                                                                listBottomPadding,
                                                                        ),
                                                                selectedLetter = selectedLetter,
                                                                onLetterSelected = { letter ->
                                                                    if (selectedLetter != letter) {
                                                                        selectedLetter = letter
                                                                        scrollToLetter(letter)
                                                                    }
                                                                },
                                                                onDragStart = {
                                                                    showLetterIndicator = true
                                                                },
                                                                onDragEnd = {
                                                                    showLetterIndicator = false
                                                                },
                                                                appColors = appColors,
                                                                currentAccent = currentAccent,
                                                            )

                                                            // 屏幕中央大字显示气泡
                                                            androidx.compose.animation
                                                                .AnimatedVisibility(
                                                                    visible =
                                                                        showLetterIndicator &&
                                                                            selectedLetter != null,
                                                                    enter =
                                                                        fadeIn(
                                                                            animationSpec =
                                                                                tween(100)
                                                                        ),
                                                                    exit =
                                                                        fadeOut(
                                                                            animationSpec =
                                                                                tween(150)
                                                                        ),
                                                                    modifier =
                                                                        Modifier.align(
                                                                            Alignment.Center
                                                                        ),
                                                                ) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier.size(80.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                    currentAccent
                                                                                        .mainColor
                                                                                        .copy(
                                                                                            alpha =
                                                                                                0.85f
                                                                                        )
                                                                                ),
                                                                        contentAlignment =
                                                                            Alignment.Center,
                                                                    ) {
                                                                        Text(
                                                                            text =
                                                                                selectedLetter
                                                                                    ?.toString()
                                                                                    ?: "",
                                                                            color = Color.White,
                                                                            fontSize = 38.sp,
                                                                            fontWeight =
                                                                                FontWeight.Bold,
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Screen.Playlists -> {
                                    // PLAYLISTS TAB
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        LazyColumn(
                                            modifier =
                                                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding =
                                                PaddingValues(bottom = listBottomPadding),
                                        ) {
                                            // 1. Fixed Playlist Cards (Favorites & Blacklist) in a
                                            // single row
                                            item {
                                                Row(
                                                    modifier =
                                                        Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(8.dp),
                                                ) {
                                                    CommonItemCard(
                                                        cover = {
                                                            Box(
                                                                modifier =
                                                                    Modifier.size(42.dp)
                                                                        .clip(
                                                                            RoundedCornerShape(
                                                                                12.dp
                                                                            )
                                                                        )
                                                                        .background(
                                                                            if (isDarkMode)
                                                                                Color.White.copy(
                                                                                    alpha = 0.1f
                                                                                )
                                                                            else Color(0x0F000000)
                                                                        ),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default.Favorite,
                                                                    contentDescription = "我喜欢的音乐",
                                                                    tint = Color(0xFFE06C75),
                                                                    modifier = Modifier.size(21.dp),
                                                                )
                                                            }
                                                        },
                                                        title = "我喜欢的音乐",
                                                        subtitleText = "$favoriteSongsCount 首歌曲",
                                                        appColors = appColors,
                                                        onClick = {
                                                            activePlaylistId = -1L
                                                            activePlaylistName = "我喜欢的音乐"
                                                            viewModel.loadFavoriteSongs()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    )

                                                    CommonItemCard(
                                                        cover = {
                                                            Box(
                                                                modifier =
                                                                    Modifier.size(42.dp)
                                                                        .clip(
                                                                            RoundedCornerShape(
                                                                                12.dp
                                                                            )
                                                                        )
                                                                        .background(
                                                                            if (isDarkMode)
                                                                                Color.White.copy(
                                                                                    alpha = 0.1f
                                                                                )
                                                                            else Color(0x0F000000)
                                                                        ),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default
                                                                            .HourglassEmpty,
                                                                    contentDescription = "遗忘的沙漏",
                                                                    tint =
                                                                        if (isDarkMode)
                                                                            Color(0xFFE5C07B)
                                                                                .copy(alpha = 0.8f)
                                                                        else Color(0xFFE5C07B),
                                                                    modifier = Modifier.size(21.dp),
                                                                )
                                                            }
                                                        },
                                                        title = "遗忘的沙漏",
                                                        subtitleText = "$blacklistSongsCount 首歌曲",
                                                        appColors = appColors,
                                                        onClick = {
                                                            activePlaylistId = -2L
                                                            activePlaylistName = "遗忘的沙漏"
                                                            viewModel.loadBlacklistSongs()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                            }

                                            // 3. Custom Playlists Header
                                            if (playlists.isNotEmpty()) {
                                                item {
                                                    Text(
                                                        text = "自定义歌单 (${playlists.size})",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appColors.textColorSecondary,
                                                        modifier = Modifier.padding(top = 4.dp),
                                                    )
                                                }

                                                items(playlists, key = { it.id }) { playlist ->
                                                    CommonItemCard(
                                                        cover = {
                                                            PlaylistCover(
                                                                firstSongId = playlist.firstSongId,
                                                                currentAccent = currentAccent,
                                                                modifier = Modifier.size(42.dp),
                                                            )
                                                        },
                                                        title = playlist.name,
                                                        subtitleText = "${playlist.songCount} 首歌曲",
                                                        appColors = appColors,
                                                        onClick = {
                                                            activePlaylistId = playlist.id
                                                            activePlaylistName = playlist.name
                                                            viewModel.loadSongsInPlaylist(
                                                                playlist.id
                                                            )
                                                        },
                                                        actionArea = {
                                                            IconButton(
                                                                onClick = {
                                                                    playlistToDelete = playlist
                                                                },
                                                                modifier = Modifier.size(36.dp),
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Default.Delete,
                                                                    contentDescription = "删除歌单",
                                                                    tint =
                                                                        appColors
                                                                            .textColorSecondary,
                                                                    modifier = Modifier.size(18.dp),
                                                                )
                                                            }
                                                        },
                                                    )
                                                }
                                            } else {
                                                if (isPlaylistsLoaded) {
                                                    item {
                                                        Box(
                                                            modifier =
                                                                Modifier.fillMaxWidth()
                                                                    .height(120.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text(
                                                                text = "暂无自定义歌单\n点击右上角 '+' 按钮创建",
                                                                color =
                                                                    appColors.textColorSecondary,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    item {
                                                        Box(
                                                            modifier =
                                                                Modifier.fillMaxWidth()
                                                                    .height(120.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Screen.Artists -> {
                                    // ARTISTS TAB

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        if (artistDisplayList.isEmpty()) {
                                            if (isSongsLoaded) {
                                                Box(
                                                    modifier =
                                                        Modifier.fillMaxWidth()
                                                            .weight(1.0f)
                                                            .padding(horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "暂无歌手信息。请先扫描歌曲。",
                                                        color = appColors.textColorSecondary,
                                                        fontSize = 14.sp,
                                                    )
                                                }
                                            } else {
                                                Box(modifier = Modifier.fillMaxWidth().weight(1.0f))
                                            }
                                        } else {
                                            var selectedLetter by remember {
                                                mutableStateOf<Char?>(null)
                                            }
                                            var showLetterIndicator by remember {
                                                mutableStateOf(false)
                                            }
                                            val alphabet = remember { ('A'..'Z').toList() + '#' }
                                            var alphabetHeight by remember { mutableStateOf(1f) }

                                            val firstVisibleIndex by remember {
                                                derivedStateOf {
                                                    artistListState.firstVisibleItemIndex
                                                }
                                            }
                                            LaunchedEffect(firstVisibleIndex) {
                                                if (
                                                    !showLetterIndicator &&
                                                        artistDisplayList.isNotEmpty()
                                                ) {
                                                    val visibleItem =
                                                        artistDisplayList.getOrNull(
                                                            firstVisibleIndex
                                                        )
                                                    val currentLetter =
                                                        when (visibleItem) {
                                                            is Char -> visibleItem
                                                            is ArtistItem -> visibleItem.letter
                                                            else -> null
                                                        }
                                                    if (currentLetter != null) {
                                                        selectedLetter = currentLetter
                                                    }
                                                }
                                            }

                                            val scrollToLetter = { letter: Char ->
                                                val targetIndex =
                                                    getTargetIndex(letter, artistIndexMap)
                                                if (targetIndex != -1) {
                                                    coroutineScope.launch {
                                                        artistListState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }

                                            Box(modifier = Modifier.fillMaxWidth().weight(1.0f)) {
                                                // 歌手列表
                                                LazyColumn(
                                                    state = artistListState,
                                                    modifier =
                                                        Modifier.fillMaxSize()
                                                            .padding(start = 16.dp, end = 36.dp),
                                                    verticalArrangement =
                                                        Arrangement.spacedBy(8.dp),
                                                    contentPadding =
                                                        PaddingValues(bottom = listBottomPadding),
                                                ) {
                                                    itemsIndexed(artistDisplayList) { index, item ->
                                                        when (item) {
                                                            is Char -> {
                                                                CharacterHeader(
                                                                    letter = item,
                                                                    appColors = appColors,
                                                                )
                                                            }

                                                            is ArtistItem -> {
                                                                CommonItemCard(
                                                                    cover = {
                                                                        val firstSong =
                                                                            item.songs.firstOrNull()
                                                                        if (firstSong != null) {
                                                                            SongCover(
                                                                                song = firstSong,
                                                                                isCurrent = false,
                                                                                isPlaying = false,
                                                                                modifier =
                                                                                    Modifier.size(
                                                                                        42.dp
                                                                                    ),
                                                                                shape =
                                                                                    RoundedCornerShape(
                                                                                        12.dp
                                                                                    ),
                                                                                fallbackIcon =
                                                                                    Icons.Default
                                                                                        .Person,
                                                                            )
                                                                        } else {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier.size(
                                                                                            42.dp
                                                                                        )
                                                                                        .clip(
                                                                                            RoundedCornerShape(
                                                                                                12
                                                                                                    .dp
                                                                                            )
                                                                                        )
                                                                                        .background(
                                                                                            currentAccent
                                                                                                .mainColor
                                                                                                .copy(
                                                                                                    alpha =
                                                                                                        0.15f
                                                                                                )
                                                                                        ),
                                                                                contentAlignment =
                                                                                    Alignment.Center,
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector =
                                                                                        Icons
                                                                                            .Default
                                                                                            .Person,
                                                                                    contentDescription =
                                                                                        null,
                                                                                    tint =
                                                                                        currentAccent
                                                                                            .mainColor,
                                                                                    modifier =
                                                                                        Modifier
                                                                                            .size(
                                                                                                24
                                                                                                    .dp
                                                                                            ),
                                                                                )
                                                                            }
                                                                        }
                                                                    },
                                                                    title = item.displayName,
                                                                    subtitleText =
                                                                        "${item.songs.size} 首歌曲",
                                                                    appColors = appColors,
                                                                    onClick = {
                                                                        activeArtistName =
                                                                            item.artistKey
                                                                    },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // 右侧 A-Z 定位条
                                                AlphabetIndexer(
                                                    modifier =
                                                        Modifier.align(Alignment.CenterEnd)
                                                            .width(16.dp)
                                                            .padding(
                                                                end = 4.dp,
                                                                bottom = listBottomPadding,
                                                            ),
                                                    selectedLetter = selectedLetter,
                                                    onLetterSelected = { letter ->
                                                        if (selectedLetter != letter) {
                                                            selectedLetter = letter
                                                            scrollToLetter(letter)
                                                        }
                                                    },
                                                    onDragStart = { showLetterIndicator = true },
                                                    onDragEnd = { showLetterIndicator = false },
                                                    appColors = appColors,
                                                    currentAccent = currentAccent,
                                                )

                                                // 屏幕正中央大字字母提示气泡
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible =
                                                        showLetterIndicator &&
                                                            selectedLetter != null,
                                                    enter = fadeIn(animationSpec = tween(100)),
                                                    exit = fadeOut(animationSpec = tween(150)),
                                                    modifier = Modifier.align(Alignment.Center),
                                                ) {
                                                    Box(
                                                        modifier =
                                                            Modifier.size(80.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    currentAccent.mainColor.copy(
                                                                        alpha = 0.85f
                                                                    )
                                                                ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = selectedLetter?.toString() ?: "",
                                                            color = Color.White,
                                                            fontSize = 38.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Selection Action Bar for batch operations
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isMultiSelectMode && selectedSongs.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Card(
                            modifier =
                                Modifier.padding(16.dp)
                                    .navigationBarsPadding()
                                    .border(
                                        width = 0.5.dp,
                                        color =
                                            if (isDarkMode) Color.White.copy(alpha = 0.12f)
                                            else Color.Black.copy(alpha = 0.08f),
                                        shape = DialogShape,
                                    ),
                            shape = DialogShape,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (isDarkMode) Color(0xE61E1E22) else Color(0xE6FFFFFF)
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val currentPlaylistId = lastActivePlaylistId
                                when {
                                    activePlaylistId != null && currentPlaylistId == -2L -> {
                                        BatchActionButton(
                                            icon = Icons.Default.Refresh,
                                            label = "恢复",
                                            tint = currentAccent.mainColor,
                                            onClick = {
                                                viewModel.removeSongsFromBlacklist(
                                                    selectedSongs.toList()
                                                )
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                                Toast.makeText(
                                                        context,
                                                        "已批量从遗忘的沙漏恢复",
                                                        Toast.LENGTH_SHORT,
                                                    )
                                                    .show()
                                            },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.Delete,
                                            label = "删除",
                                            tint = Color(0xFFE06C75),
                                            onClick = { songsToDelete = selectedSongs.toList() },
                                        )
                                    }

                                    activePlaylistId != null &&
                                        currentPlaylistId != null &&
                                        currentPlaylistId > 0 -> {
                                        BatchActionButton(
                                            icon = Icons.Default.PlayArrow,
                                            label = "播放",
                                            tint = currentAccent.mainColor,
                                            onClick = {
                                                viewModel.setPlaybackQueue(selectedSongs.toList())
                                                viewModel.playSong(context, selectedSongs[0])
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                        )
                                        val anyNotFav =
                                            selectedSongs.isEmpty() ||
                                                selectedSongs.any { !it.isFavorite }
                                        BatchActionButton(
                                            icon =
                                                if (anyNotFav) Icons.Default.FavoriteBorder
                                                else Icons.Default.Favorite,
                                            label = if (anyNotFav) "喜欢" else "取消喜欢",
                                            tint = Color(0xFFE06C75),
                                            onClick = {
                                                viewModel.toggleFavoriteBatch(
                                                    selectedSongs.toList(),
                                                    anyNotFav,
                                                )
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.Clear,
                                            label = "移除",
                                            tint = appColors.textColorPrimary,
                                            onClick = {
                                                viewModel.removeSongsFromPlaylist(
                                                    currentPlaylistId!!,
                                                    selectedSongs.map { it.id },
                                                )
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                                Toast.makeText(
                                                        context,
                                                        "已批量从歌单中移除",
                                                        Toast.LENGTH_SHORT,
                                                    )
                                                    .show()
                                            },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.HourglassEmpty,
                                            label = "移入沙漏",
                                            tint = Color(0xFFE5C07B),
                                            onClick = { songsToBlacklist = selectedSongs.toList() },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.Delete,
                                            label = "删除",
                                            tint = Color(0xFFE06C75),
                                            onClick = { songsToDelete = selectedSongs.toList() },
                                        )
                                    }

                                    else -> {
                                        BatchActionButton(
                                            icon = Icons.Default.PlayArrow,
                                            label = "播放",
                                            tint = currentAccent.mainColor,
                                            onClick = {
                                                viewModel.setPlaybackQueue(selectedSongs.toList())
                                                viewModel.playSong(context, selectedSongs[0])
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                        )
                                        val anyNotFav =
                                            selectedSongs.isEmpty() ||
                                                selectedSongs.any { !it.isFavorite }
                                        BatchActionButton(
                                            icon =
                                                if (anyNotFav) Icons.Default.FavoriteBorder
                                                else Icons.Default.Favorite,
                                            label = if (anyNotFav) "喜欢" else "取消喜欢",
                                            tint = Color(0xFFE06C75),
                                            onClick = {
                                                viewModel.toggleFavoriteBatch(
                                                    selectedSongs.toList(),
                                                    anyNotFav,
                                                )
                                                isMultiSelectMode = false
                                                selectedSongs.clear()
                                            },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.Add,
                                            label = "加歌单",
                                            tint = appColors.textColorPrimary,
                                            onClick = { showBatchAddToPlaylistDialog = true },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.HourglassEmpty,
                                            label = "移入沙漏",
                                            tint = Color(0xFFE5C07B),
                                            onClick = { songsToBlacklist = selectedSongs.toList() },
                                        )
                                        BatchActionButton(
                                            icon = Icons.Default.Delete,
                                            label = "删除",
                                            tint = Color(0xFFE06C75),
                                            onClick = { songsToDelete = selectedSongs.toList() },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Fixed Docked Mini Player
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentSong != null && !isMultiSelectMode,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        currentSong?.let { song ->
                            val queue by viewModel.playbackQueue.collectAsState()
                            val currentSongIndex = queue.indexOfFirst { it.id == song.id }
                            val prevSong =
                                if (currentSongIndex != -1 && queue.isNotEmpty()) {
                                    queue[(currentSongIndex - 1 + queue.size) % queue.size]
                                } else null
                            val nextSong =
                                if (currentSongIndex != -1 && queue.isNotEmpty()) {
                                    queue[(currentSongIndex + 1) % queue.size]
                                } else null

                            val offsetVal = horizontalOffset.value
                            val progress =
                                (kotlin.math.abs(offsetVal) / swipeThresholdPx).coerceIn(0f, 1f)
                            val isOverThreshold = kotlin.math.abs(offsetVal) >= swipeThresholdPx
                            val hasMultipleSongs = queue.size > 1

                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .background(
                                            if (isDarkMode) Color(0xFF131316) else Color(0xFFF0F0F2)
                                        )
                            ) {
                                // 背景层提示
                                if (offsetVal > 0f) {
                                    // 右滑上一首
                                    Row(
                                        modifier =
                                            Modifier.align(Alignment.CenterStart)
                                                .height(68.dp)
                                                .padding(start = 20.dp)
                                                .graphicsLayer {
                                                    alpha = progress
                                                    translationX = (progress - 1f) * 40f
                                                },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = null,
                                            tint =
                                                if (isOverThreshold && hasMultipleSongs)
                                                    currentAccent.mainColor
                                                else appColors.textColorSecondary,
                                            modifier =
                                                Modifier.size(24.dp).graphicsLayer {
                                                    val scale =
                                                        if (isOverThreshold && hasMultipleSongs)
                                                            1.2f
                                                        else 1.0f
                                                    scaleX = scale
                                                    scaleY = scale
                                                },
                                        )
                                        Column {
                                            Text(
                                                text =
                                                    if (!hasMultipleSongs) "当前队列仅一首歌"
                                                    else if (isOverThreshold) "释放播放上一首"
                                                    else "右滑上一首",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color =
                                                    if (isOverThreshold && hasMultipleSongs)
                                                        currentAccent.mainColor
                                                    else appColors.textColorPrimary,
                                            )
                                            prevSong?.let {
                                                if (hasMultipleSongs) {
                                                    Text(
                                                        text = it.title,
                                                        fontSize = 10.sp,
                                                        color = appColors.textColorSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.widthIn(max = 180.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (offsetVal < 0f) {
                                    // 左滑下一首
                                    Row(
                                        modifier =
                                            Modifier.align(Alignment.CenterEnd)
                                                .height(68.dp)
                                                .padding(end = 20.dp)
                                                .graphicsLayer {
                                                    alpha = progress
                                                    translationX = (1f - progress) * 40f
                                                },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text =
                                                    if (!hasMultipleSongs) "当前队列仅一首歌"
                                                    else if (isOverThreshold) "释放播放下一首"
                                                    else "左滑下一首",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color =
                                                    if (isOverThreshold && hasMultipleSongs)
                                                        currentAccent.mainColor
                                                    else appColors.textColorPrimary,
                                            )
                                            nextSong?.let {
                                                if (hasMultipleSongs) {
                                                    Text(
                                                        text = it.title,
                                                        fontSize = 10.sp,
                                                        color = appColors.textColorSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.widthIn(max = 180.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = null,
                                            tint =
                                                if (isOverThreshold && hasMultipleSongs)
                                                    currentAccent.mainColor
                                                else appColors.textColorSecondary,
                                            modifier =
                                                Modifier.size(24.dp).graphicsLayer {
                                                    val scale =
                                                        if (isOverThreshold && hasMultipleSongs)
                                                            1.2f
                                                        else 1.0f
                                                    scaleX = scale
                                                    scaleY = scale
                                                },
                                        )
                                    }
                                }

                                Card(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .graphicsLayer { translationX = offsetVal }
                                            .clickable(
                                                enabled =
                                                    kotlin.math.abs(offsetVal) < 5f &&
                                                        dragDirection == null,
                                                onClick = { showFullPlayer = true },
                                            )
                                            .then(miniPlayerDragModifier),
                                    shape = RoundedCornerShape(0.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                if (isDarkMode) Color(0xFF161619)
                                                else Color(0xFFFFFFFF)
                                        ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
                                            Box(
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(
                                                            if (isDarkMode) Color(0x1AFFFFFF)
                                                            else Color(0x0D000000)
                                                        )
                                                        .align(Alignment.TopCenter)
                                            )

                                            val progress =
                                                if (song.duration > 0) {
                                                    playbackProgress.toFloat() /
                                                        song.duration.toFloat()
                                                } else {
                                                    0f
                                                }
                                            Box(
                                                modifier =
                                                    Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                                                        .height(2.5.dp)
                                                        .background(currentAccent.mainColor)
                                                        .align(Alignment.TopStart)
                                            )

                                            Row(
                                                modifier =
                                                    Modifier.fillMaxSize()
                                                        .padding(start = 26.dp, end = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                SongCover(
                                                    song = song,
                                                    isCurrent = false,
                                                    isPlaying = false,
                                                    modifier = Modifier.size(42.dp),
                                                )

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1.0f)) {
                                                    Text(
                                                        text = song.title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appColors.textColorPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    val quality = song.getQualityType()
                                                    Row(
                                                        verticalAlignment =
                                                            Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                            Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        if (quality != QualityType.SQ) {
                                                            QualityBadge(
                                                                quality = quality,
                                                                isDarkMode = isDarkMode,
                                                            )
                                                        }
                                                        Text(
                                                            text = song.artist,
                                                            fontSize = 11.sp,
                                                            color = appColors.textColorSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = {
                                                        if (isPlaying) {
                                                            viewModel.pauseSong()
                                                        } else {
                                                            viewModel.resumeSong()
                                                        }
                                                    },
                                                    modifier =
                                                        Modifier.size(40.dp)
                                                            .clip(RoundedCornerShape(20.dp)),
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            if (isPlaying) Icons.Default.Pause
                                                            else Icons.Default.PlayArrow,
                                                        contentDescription = "Play/Pause",
                                                        tint = currentAccent.mainColor,
                                                        modifier = Modifier.size(24.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.navigationBarsPadding())
                                    }
                                }
                            }
                        }
                    }

                    val backToTopBottomPadding by
                        animateDpAsState(
                            targetValue = if (currentSong != null) 84.dp else 16.dp,
                            label = "back_to_top_padding",
                        )

                    val locateInteractionSource = remember { MutableInteractionSource() }
                    val isLocatePressed by locateInteractionSource.collectIsPressedAsState()
                    val locateAlpha by
                        animateFloatAsState(
                            targetValue = if (isLocatePressed) 1.0f else 0.92f,
                            label = "locate_alpha",
                        )
                    val locateScale by
                        animateFloatAsState(
                            targetValue = if (isLocatePressed) 0.86f else 1.0f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            label = "locate_scale",
                        )

                    val topInteractionSource = remember { MutableInteractionSource() }
                    val isTopPressed by topInteractionSource.collectIsPressedAsState()
                    val topAlpha by
                        animateFloatAsState(
                            targetValue = if (isTopPressed) 1.0f else 0.92f,
                            label = "top_alpha",
                        )
                    val topScale by
                        animateFloatAsState(
                            targetValue = if (isTopPressed) 0.86f else 1.0f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            label = "top_scale",
                        )

                    val locateOffset by
                        animateDpAsState(
                            targetValue = if (showBackToTop) 52.dp else 0.dp,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            label = "locate_offset",
                        )

                    val buttonBorder =
                        Modifier.border(
                            width = 1.dp,
                            color =
                                if (isDarkMode) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.08f),
                            shape = CircleShape,
                        )

                    Box(
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = backToTopBottomPadding)
                                .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // 1. 定位当前歌曲按钮
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showLocateSong && !isAtBottom,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier =
                                Modifier.padding(
                                    bottom = if (locateOffset < 0.dp) 0.dp else locateOffset
                                ),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(40.dp)
                                        .graphicsLayer {
                                            alpha = locateAlpha
                                            scaleX = locateScale
                                            scaleY = locateScale
                                        }
                                        .clip(CircleShape)
                                        .background(currentAccent.mainColor)
                                        .then(buttonBorder)
                                        .clickable(
                                            interactionSource = locateInteractionSource,
                                            indication =
                                                androidx.compose.material3.ripple(
                                                    color = Color.White
                                                ),
                                            onClick = { locateCurrentSong() },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "定位当前歌曲",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize()
                                            .background(
                                                if (isLocatePressed) Color.White.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                )
                            }
                        }

                        // 2. 回到顶部按钮
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBackToTop && !isAtBottom,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(40.dp)
                                        .graphicsLayer {
                                            alpha = topAlpha
                                            scaleX = topScale
                                            scaleY = topScale
                                        }
                                        .clip(CircleShape)
                                        .background(currentAccent.mainColor)
                                        .then(buttonBorder)
                                        .clickable(
                                            interactionSource = topInteractionSource,
                                            indication =
                                                androidx.compose.material3.ripple(
                                                    color = Color.White
                                                ),
                                            onClick = {
                                                coroutineScope.launch {
                                                    activeListState?.scrollToItem(0)
                                                }
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "回到顶部",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize()
                                            .background(
                                                if (isTopPressed) Color.White.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 0.0 Sort Order Selection Dialog
    if (showSortOrderDialog) {
        val itemBg = if (isDarkMode) Color(0x0CFFFFFF) else Color(0x06000000)
        val currentSortOrder by viewModel.sortOrder.collectAsState()

        AppDialog(
            onDismissRequest = { showSortOrderDialog = false },
            title = "选择排序方式",
            icon = Icons.Default.Sort,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
            ) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSortAscending) currentAccent.mainColor.copy(alpha = 0.15f)
                                else itemBg
                            )
                            .clickable { viewModel.setSortAscending(true) }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "正序",
                        color =
                            if (isSortAscending) currentAccent.mainColor
                            else appColors.textColorPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!isSortAscending) currentAccent.mainColor.copy(alpha = 0.15f)
                                else itemBg
                            )
                            .clickable { viewModel.setSortAscending(false) }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "倒序",
                        color =
                            if (!isSortAscending) currentAccent.mainColor
                            else appColors.textColorPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SortOrder.values().forEach { order ->
                    val isSelected = order == currentSortOrder
                    val accentItemBg =
                        if (isSelected) {
                            currentAccent.mainColor.copy(alpha = 0.12f)
                        } else {
                            appColors.cardBackground
                        }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentItemBg)
                                .border(
                                    1.5.dp,
                                    if (isSelected) currentAccent.mainColor else Color.Transparent,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { viewModel.setSortOrder(order) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text =
                                when (order) {
                                    SortOrder.BY_TITLE -> "按歌名排序"
                                    SortOrder.BY_DATE_ADDED -> "按创建时间排序"
                                    SortOrder.BY_ADD_TIME -> "按扫描时间排序"
                                    SortOrder.BY_ARTIST -> "按歌手排序"
                                    SortOrder.BY_DURATION -> "按时长排序"
                                },
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = currentAccent.mainColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // 0.1 Blocked Folders Dialog
    if (showBlockedFoldersDialog) {
        val itemBg = if (isDarkMode) Color(0x0AFFFFFF) else Color(0x0A000000)
        val expandedStates = remember {
            androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
        }
        val flatList =
            remember(folderTree, expandedStates.toMap()) {
                val result = mutableListOf<FlatFolderItem>()
                fun flatten(nodes: List<FolderNode>, depth: Int) {
                    nodes.forEach { node ->
                        val isExpanded = expandedStates[node.absolutePath] ?: true
                        result.add(FlatFolderItem(node, depth, isExpanded))
                        if (isExpanded && node.children.isNotEmpty()) {
                            flatten(node.children, depth + 1)
                        }
                    }
                }
                flatten(folderTree, 0)
                result
            }

        AppDialog(
            onDismissRequest = { showBlockedFoldersDialog = false },
            title = "屏蔽设置",
            icon = Icons.Default.Delete,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(
                    onClick = { showBlockedFoldersDialog = false },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
            ) {
                if (flatList.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无检测到的文件夹",
                                color = appColors.textColorSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    items(items = flatList, key = { it.node.absolutePath }) { item ->
                        val node = item.node
                        val depth = item.depth
                        val isExpanded = item.isExpanded

                        FolderRow(
                            node = node,
                            depth = depth,
                            isExpanded = isExpanded,
                            isDarkMode = isDarkMode,
                            onToggleExpand = { expandedStates[node.absolutePath] = !isExpanded },
                            onBlockClick = { viewModel.addBlockedFolder(node.absolutePath) },
                            onUnblockClick = { viewModel.removeBlockedFolder(node.absolutePath) },
                            appColors = appColors,
                            currentAccent = currentAccent,
                        )
                    }
                }
            }
        }
    }

    // 1. Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AppDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = "创建新歌单",
            icon = Icons.Default.PlaylistAdd,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
        ) {
            // Input TextField
            OutlinedTextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                placeholder = {
                    Text(
                        "输入歌单名称...",
                        color = appColors.textColorSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (newPlaylistName.isNotEmpty()) {
                        IconButton(
                            onClick = { newPlaylistName = "" },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清空",
                                tint = appColors.textColorSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor =
                            if (isDarkMode) Color(0xFF222225) else Color(0xFFF3F3F5),
                        unfocusedContainerColor =
                            if (isDarkMode) Color(0xFF1B1B1E) else Color(0xFFF9F9FA),
                        focusedTextColor = appColors.textColorPrimary,
                        unfocusedTextColor = appColors.textColorPrimary,
                        focusedBorderColor = currentAccent.mainColor,
                        unfocusedBorderColor =
                            if (isDarkMode) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.06f),
                        cursorColor = currentAccent.mainColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            // Action Buttons
            val isNameValid = newPlaylistName.isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
            ) {
                // Cancel Button
                Button(
                    onClick = {
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Create Button
                Button(
                    enabled = isNameValid,
                    onClick = {
                        viewModel.createPlaylist(newPlaylistName.trim())
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = currentAccent.mainColor,
                            contentColor = Color.White,
                            disabledContainerColor =
                                if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFE5E5E9),
                            disabledContentColor = appColors.textColorSecondary.copy(alpha = 0.4f),
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("创建", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 1.1 Delete Playlist Confirm Dialog
    if (playlistToDelete != null) {
        AppDialog(
            onDismissRequest = { playlistToDelete = null },
            title = "删除歌单",
            icon = Icons.Default.Delete,
            iconColor = Color(0xFFE06C75),
            appColors = appColors,
        ) {
            Text(
                text = "确认要删除歌单「${playlistToDelete!!.name}」吗？\n删除后歌单内的歌曲记录将丢失，该操作无法撤销。",
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Cancel Button
                Button(
                    onClick = { playlistToDelete = null },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Delete Button
                Button(
                    onClick = {
                        val id = playlistToDelete!!.id
                        viewModel.deletePlaylist(id)
                        playlistToDelete = null
                        Toast.makeText(context, "歌单已删除", Toast.LENGTH_SHORT).show()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE06C75),
                            contentColor = Color.White,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("确认删除", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 1.15 Import Playlist Result Dialog
    if (importResult != null) {
        val result = importResult!!
        val hasFailure = result.failureCount > 0

        AppDialog(
            onDismissRequest = { importResult = null },
            title = "导入歌单结果",
            icon = if (hasFailure) Icons.Default.Warning else Icons.Default.Check,
            iconColor = if (hasFailure) Color(0xFFD97706) else currentAccent.mainColor,
            appColors = appColors,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "已导入歌单/分组: ${result.playlistsImported} 个",
                    color = appColors.textColorPrimary,
                    fontSize = 13.sp,
                )
                Text(
                    text = "成功关联本地文件: ${result.successCount} 首",
                    color = currentAccent.mainColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "未找到本地物理文件: ${result.failureCount} 首",
                    color = if (hasFailure) Color(0xFFE06C75) else appColors.textColorSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (hasFailure) FontWeight.Bold else FontWeight.Normal,
                )
            }

            if (hasFailure && result.failedSongs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "未找到物理文件的歌曲列表:",
                    color = appColors.textColorSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDarkMode) Color.White.copy(alpha = 0.05f)
                                else Color.Black.copy(alpha = 0.03f)
                            )
                            .border(
                                0.5.dp,
                                if (isDarkMode) Color.White.copy(alpha = 0.08f)
                                else Color.Black.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(result.failedSongs) { songText ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE06C75))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = songText,
                                    color = appColors.textColorPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Button(
                onClick = { importResult = null },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = currentAccent.mainColor,
                        contentColor = Color.White,
                    ),
                shape = DialogButtonShape,
                modifier = Modifier.fillMaxWidth().height(DialogButtonHeight),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("我知道了", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 1.16 Import Playlist Error Dialog
    if (importError != null) {
        AppDialog(
            onDismissRequest = { importError = null },
            title = "导入歌单失败",
            icon = Icons.Default.Warning,
            iconColor = Color(0xFFE06C75),
            appColors = appColors,
        ) {
            Text(
                text = importError!!,
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Button(
                onClick = { importError = null },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE06C75),
                        contentColor = Color.White,
                    ),
                shape = DialogButtonShape,
                modifier = Modifier.fillMaxWidth().height(DialogButtonHeight),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("我知道了", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 1.3 Delete Song Confirm Dialog
    if (songToDelete != null) {
        AppDialog(
            onDismissRequest = { songToDelete = null },
            title = "删除歌曲",
            icon = Icons.Default.Delete,
            iconColor = Color(0xFFE06C75),
            titleColor = Color(0xFFE06C75),
            appColors = appColors,
        ) {
            Text(
                text = "确定要删除歌曲「${songToDelete!!.title}」吗？",
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Cancel Button
                Button(
                    onClick = { songToDelete = null },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Delete Button
                Button(
                    onClick = {
                        val song = songToDelete!!
                        viewModel.deleteSong(context, song.id)
                        songToDelete = null
                        Toast.makeText(context, "已删除歌曲记录", Toast.LENGTH_SHORT).show()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE06C75),
                            contentColor = Color.White,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("确认删除", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Batch Delete Confirm Dialog
    if (songsToDelete != null) {
        AppDialog(
            onDismissRequest = { songsToDelete = null },
            title = "批量删除歌曲",
            icon = Icons.Default.Delete,
            iconColor = Color(0xFFE06C75),
            titleColor = Color(0xFFE06C75),
            appColors = appColors,
        ) {
            Text(
                text = "确定要删除选中的 ${songsToDelete!!.size} 首歌曲吗？\n删除后本地歌曲记录将丢失，无法撤销。",
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { songsToDelete = null },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        val ids = songsToDelete!!.map { it.id }
                        viewModel.deleteSongs(context, ids)
                        songsToDelete = null
                        isMultiSelectMode = false
                        selectedSongs.clear()
                        Toast.makeText(context, "已批量删除歌曲记录", Toast.LENGTH_SHORT).show()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE06C75),
                            contentColor = Color.White,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("确认删除", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Batch Blacklist Confirm Dialog
    if (songsToBlacklist != null) {
        AppDialog(
            onDismissRequest = { songsToBlacklist = null },
            title = "移入遗忘的沙漏",
            icon = Icons.Default.HourglassEmpty,
            iconColor = Color(0xFFE5C07B),
            appColors = appColors,
        ) {
            Text(
                text = "确认要将选中的 ${songsToBlacklist!!.size} 首歌曲移至遗忘的沙漏吗？\n移入后歌曲将自动停止播放且不再在歌库中显示。",
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { songsToBlacklist = null },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        viewModel.addSongsToBlacklist(context, songsToBlacklist!!)
                        songsToBlacklist = null
                        isMultiSelectMode = false
                        selectedSongs.clear()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5C07B),
                            contentColor = Color.White,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("确认移入", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Batch Add To Playlist Dialog
    if (showBatchAddToPlaylistDialog) {
        val itemBg = if (isDarkMode) Color(0x0AFFFFFF) else Color(0x0A000000)

        AppDialog(
            onDismissRequest = { showBatchAddToPlaylistDialog = false },
            title = "批量加入到歌单",
            icon = Icons.Default.PlaylistAdd,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(
                    onClick = { showBatchAddToPlaylistDialog = false },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        ) {
            if (playlists.isEmpty()) {
                Text(
                    text = "暂无自定义歌单。\n请先前往“我的歌单”创建新歌单。",
                    color = appColors.textColorSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                    modifier = Modifier.heightIn(max = 220.dp),
                ) {
                    items(playlists) { playlist ->
                        CommonItemCard(
                            cover = {
                                PlaylistCover(
                                    firstSongId = playlist.firstSongId,
                                    currentAccent = currentAccent,
                                    modifier = Modifier.size(42.dp),
                                )
                            },
                            title = playlist.name,
                            subtitleText = "${playlist.songCount} 首歌曲",
                            appColors = appColors,
                            onClick = {
                                viewModel.addSongsToPlaylist(
                                    playlist.id,
                                    selectedSongs.map { it.id },
                                )
                                Toast.makeText(
                                        context,
                                        "已成功批量加入: ${playlist.name}",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                                showBatchAddToPlaylistDialog = false
                                isMultiSelectMode = false
                                selectedSongs.clear()
                            },
                            containerColor = itemBg,
                        )
                    }
                }
            }
        }
    }

    // 1.2 Remove Song From Playlist Confirm Dialog
    if (songToRemoveFromPlaylist != null) {
        AppDialog(
            onDismissRequest = { songToRemoveFromPlaylist = null },
            title = "从歌单移除",
            icon = Icons.Default.Delete,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
        ) {
            Text(
                text = "确认要将歌曲「${songToRemoveFromPlaylist!!.second.title}」从当前歌单中移出吗？",
                color = appColors.textColorPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Cancel Button
                Button(
                    onClick = { songToRemoveFromPlaylist = null },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                            contentColor = appColors.textColorSecondary,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Remove Button
                Button(
                    onClick = {
                        val playlistId = songToRemoveFromPlaylist!!.first
                        val songId = songToRemoveFromPlaylist!!.second.id
                        viewModel.removeSongFromPlaylist(playlistId, songId)
                        songToRemoveFromPlaylist = null
                        Toast.makeText(context, "已从歌单移除", Toast.LENGTH_SHORT).show()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE06C75),
                            contentColor = Color.White,
                        ),
                    shape = DialogButtonShape,
                    modifier = Modifier.weight(1f).height(DialogButtonHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("确认移除", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 2. Add Song To Playlist Dialog
    if (showAddToPlaylistDialog && selectedSongForAddToPlaylist != null) {
        val itemBg = if (isDarkMode) Color(0x0AFFFFFF) else Color(0x0A000000)

        AppDialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                selectedSongForAddToPlaylist = null
            },
            title = "加入到歌单",
            icon = Icons.Default.PlaylistAdd,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(
                    onClick = {
                        showAddToPlaylistDialog = false
                        selectedSongForAddToPlaylist = null
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        ) {
            if (playlists.isEmpty()) {
                Text(
                    text = "暂无自定义歌单。\n请先前往“我的歌单”创建新歌单。",
                    color = appColors.textColorSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                    modifier = Modifier.heightIn(max = 220.dp),
                ) {
                    items(playlists) { playlist ->
                        CommonItemCard(
                            cover = {
                                PlaylistCover(
                                    firstSongId = playlist.firstSongId,
                                    currentAccent = currentAccent,
                                    modifier = Modifier.size(42.dp),
                                )
                            },
                            title = playlist.name,
                            subtitleText = "${playlist.songCount} 首歌曲",
                            appColors = appColors,
                            onClick = {
                                viewModel.addSongToPlaylist(
                                    playlist.id,
                                    selectedSongForAddToPlaylist!!.id,
                                )
                                Toast.makeText(
                                        context,
                                        "已成功加入: ${playlist.name}",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                                showAddToPlaylistDialog = false
                                selectedSongForAddToPlaylist = null
                            },
                            containerColor = itemBg,
                        )
                    }
                }
            }
        }
    }

    // 3. Theme Selection Dialog
    if (showThemeDialog) {
        AppDialog(
            onDismissRequest = { showThemeDialog = false },
            title = "选择主题颜色",
            icon = Icons.Default.Palette,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(onClick = { showThemeDialog = false }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
                ) {
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isDarkMode) currentAccent.mainColor.copy(alpha = 0.15f)
                                    else appColors.cardBackground
                                )
                                .border(
                                    1.5.dp,
                                    if (!isDarkMode) currentAccent.mainColor else Color.Transparent,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { viewModel.setDarkMode(false) }
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "日间模式",
                            color =
                                if (!isDarkMode) currentAccent.mainColor
                                else appColors.textColorSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDarkMode) currentAccent.mainColor.copy(alpha = 0.15f)
                                    else appColors.cardBackground
                                )
                                .border(
                                    1.5.dp,
                                    if (isDarkMode) currentAccent.mainColor else Color.Transparent,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { viewModel.setDarkMode(true) }
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "夜间模式",
                            color =
                                if (isDarkMode) currentAccent.mainColor
                                else appColors.textColorSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AccentColor.values().forEach { accent ->
                        val isSelected = accent.id == themeName
                        val accentItemBg =
                            if (isSelected) {
                                accent.mainColor.copy(alpha = 0.12f)
                            } else {
                                appColors.cardBackground
                            }
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentItemBg)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) accent.mainColor else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { viewModel.setThemeName(accent.id) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(24.dp)
                                            .clip(CircleShape)
                                            .background(accent.mainColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = accent.label,
                                    color = appColors.textColorPrimary,
                                    fontSize = 14.sp,
                                    fontWeight =
                                        if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = accent.mainColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            viewModel = viewModel,
            appColors = appColors,
            isDarkMode = isDarkMode,
            currentAccent = currentAccent,
            onDismissRequest = { showSleepTimerDialog = false },
        )
    }

    // 检查更新弹窗
    if (showUpdateDialog && latestVersionInfo != null) {
        val info = latestVersionInfo!!

        AppDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = "发现新版本",
            icon = Icons.Default.CloudDownload,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(
                    onClick = { showUpdateDialog = false },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前版本: v$currentVersionName",
                        color = appColors.textColorSecondary,
                        fontSize = 12.sp,
                    )
                    Text(text = "•", color = appColors.textColorSecondary, fontSize = 12.sp)
                    Text(
                        text = "最新版本: ${info.tagName}",
                        color = currentAccent.mainColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "更新内容：",
                    color = appColors.textColorPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .background(
                                if (isDarkMode) Color.White.copy(alpha = 0.05f)
                                else Color.Black.copy(alpha = 0.03f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = info.body.ifBlank { "无更新日志说明。" },
                            color = appColors.textColorPrimary.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DialogContentToButtonsSpace),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { showUpdateDialog = false },
                        modifier = Modifier.height(DialogButtonHeight),
                        shape = DialogButtonShape,
                    ) {
                        Text(text = "以后再说", color = appColors.textColorSecondary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(DialogButtonSpacing))
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.htmlUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = currentAccent.mainColor,
                                contentColor = Color.White,
                            ),
                        shape = DialogButtonShape,
                        modifier = Modifier.height(DialogButtonHeight),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text(text = "立即更新", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val isPlayerVisible = currentSong != null

    if (isPlayerVisible) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .graphicsLayer { translationY = playerOffsetY.value }
                    .clipToBounds()
        ) {
            currentSong?.let { song ->
                FullPlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    playbackProgress = playbackProgress,
                    viewModel = viewModel,
                    appColors = appColors,
                    isDarkMode = isDarkMode,
                    currentAccent = currentAccent,
                    onDismiss = { showFullPlayer = false },
                    onAddToPlaylistClick = { s ->
                        selectedSongForAddToPlaylist = s
                        showAddToPlaylistDialog = true
                    },
                    onNavigateToArtist = { artist ->
                        previousScreen = currentScreen
                        previousPlaylistId = activePlaylistId
                        currentScreen = Screen.Artists
                        activeArtistName = artist
                        activePlaylistId = null
                        showFullPlayer = false
                    },
                    dragModifier = playerDragModifier,
                    onSleepTimerClick = { showSleepTimerDialog = true },
                    isFullyHidden = { playerOffsetY.value >= screenHeight },
                )
            }
        }
    }
}

@Composable
private fun BatchActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

data class FlatFolderItem(val node: FolderNode, val depth: Int, val isExpanded: Boolean)

@Composable
fun FolderRow(
    node: FolderNode,
    depth: Int,
    isExpanded: Boolean,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit,
    onBlockClick: () -> Unit,
    onUnblockClick: () -> Unit,
    appColors: AppColors,
    currentAccent: AccentColor,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(),
                    onClick = {
                        if (node.children.isNotEmpty()) {
                            onToggleExpand()
                        }
                    },
                )
                .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 缩进占位 (使用 12.dp 每次缩进让空间更合理)
        val indentWidth = remember(depth) { (depth * 12 + 8).dp }
        Spacer(modifier = Modifier.width(indentWidth))

        // 2. 展开/折叠按钮
        if (node.children.isNotEmpty()) {
            Box(
                modifier =
                    Modifier.size(24.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = false),
                            onClick = onToggleExpand,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        if (isExpanded) Icons.Default.KeyboardArrowDown
                        else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = appColors.textColorSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        // 3. 文件夹图标
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint =
                if (node.isBlocked || node.isInheritedBlocked) {
                    appColors.textColorSecondary.copy(alpha = 0.5f)
                } else {
                    currentAccent.mainColor
                },
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 4. 文件夹名称
        Text(
            text = node.name,
            color =
                if (node.isBlocked || node.isInheritedBlocked) {
                    appColors.textColorSecondary
                } else {
                    appColors.textColorPrimary
                },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 5. 操作状态及按钮
        if (node.isBlocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE06C75).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "已屏蔽",
                        color = Color(0xFFE06C75),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onUnblockClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "取消屏蔽",
                        tint = Color(0xFFE06C75),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else if (node.isInheritedBlocked) {
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isDarkMode) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "随父级屏蔽",
                    color = appColors.textColorSecondary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                )
            }
        } else {
            IconButton(onClick = onBlockClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "屏蔽该文件夹",
                    tint = currentAccent.mainColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
