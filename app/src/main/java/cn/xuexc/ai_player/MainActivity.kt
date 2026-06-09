@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.xuexc.ai_player


import android.app.Activity
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import cn.xuexc.ai_player.playback.PlayMode
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.Crossfade
import cn.xuexc.ai_player.data.loadCover
import cn.xuexc.ai_player.data.loadCoverById
import cn.xuexc.ai_player.data.getCachedCover
import cn.xuexc.ai_player.data.getCachedCoverById
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cn.xuexc.ai_player.data.Playlist
import cn.xuexc.ai_player.data.Song
import cn.xuexc.ai_player.data.QualityType
import cn.xuexc.ai_player.data.getQualityType
import cn.xuexc.ai_player.data.SongItemWithLetter
import cn.xuexc.ai_player.data.ArtistItem
import cn.xuexc.ai_player.data.PinyinHelper
import cn.xuexc.ai_player.ui.ScanStatus
import cn.xuexc.ai_player.ui.SongViewModel
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import android.content.Intent
import androidx.compose.material.icons.filled.CloudDownload
import cn.xuexc.ai_player.ui.SortOrder
import cn.xuexc.ai_player.ui.theme.AIPlayerTheme
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import android.icu.text.Transliterator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

data class UpdateInfo(
    val tagName: String,
    val body: String,
    val htmlUrl: String
)

enum class Screen {
    Library, Playlists, Artists
}

// 弹窗布局规范常量
val DialogShape = RoundedCornerShape(16.dp)
val DialogHorizontalPadding = 24.dp
val DialogInnerPadding = 20.dp
val DialogTitleFontSize = 16.sp
val DialogTitleToContentSpace = 16.dp
val DialogItemSpacing = 8.dp
val DialogContentToButtonsSpace = 20.dp
val DialogButtonSpacing = 12.dp
val DialogButtonHeight = 40.dp
val DialogButtonShape = RoundedCornerShape(12.dp)

enum class AccentColor(
    val id: String, val label: String, val mainColor: Color, val gradientColors: List<Color>
) {
    TitaniumGray(
        "TitaniumGray", "钛金灰", Color(0xFF9AA5B1), listOf(Color(0xFF486581), Color(0xFFBAC7D5))
    ),
    DeepBlue(
        "DeepBlue", "极光蓝", Color(0xFF2F80ED), listOf(Color(0xFF2F80ED), Color(0xFF56CCF2))
    ),
    EmeraldGreen(
        "EmeraldGreen", "翡翠绿", Color(0xFF27AE60), listOf(Color(0xFF27AE60), Color(0xFF6FCF97))
    ),
    FlameRed(
        "FlameRed", "赤焰红", Color(0xFFFF2D55), listOf(Color(0xFFFF2D55), Color(0xFFFF5E7E))
    ),
    SakuraPink(
        "SakuraPink", "樱花粉", Color(0xFFFF8DA1), listOf(Color(0xFFFF8DA1), Color(0xFFFFC5D3))
    )
}

data class AppColors(
    val mainBackground: Brush,
    val surfaceColor: Color,
    val cardBackground: Color,
    val textColorPrimary: Color,
    val textColorSecondary: Color,
    val navBarBackground: Color,
    val navBarItemActive: Color,
    val navBarItemInactive: Color,
    val textfieldContainer: Color,
    val textfieldBorder: Color
)

fun getAppColors(accent: AccentColor, isDark: Boolean): AppColors {
    val mainColor = if (accent == AccentColor.TitaniumGray) {
        if (isDark) Color(0xFFBAC7D5) else Color(0xFF4A5568)
    } else {
        accent.mainColor
    }
    return if (isDark) {
        AppColors(
            mainBackground = SolidColor(Color(0xFF0C0C0E)),
            surfaceColor = Color(0xFF161619),
            cardBackground = Color(0x0CFFFFFF),
            textColorPrimary = Color(0xFFF5F5F7),
            textColorSecondary = Color(0xFF8E8E93),
            navBarBackground = Color(0xFF121214),
            navBarItemActive = mainColor,
            navBarItemInactive = Color(0x66FFFFFF),
            textfieldContainer = Color(0x0DFFFFFF),
            textfieldBorder = Color.Transparent
        )
    } else {
        AppColors(
            mainBackground = SolidColor(Color(0xFFF5F5F7)),
            surfaceColor = Color(0xFFFFFFFF),
            cardBackground = Color(0x06000000),
            textColorPrimary = Color(0xFF1C1C1E),
            textColorSecondary = Color(0xFF8E8E93),
            navBarBackground = Color(0xFFF1F1F3),
            navBarItemActive = mainColor,
            navBarItemInactive = Color(0x66000000),
            textfieldContainer = Color(0x06000000),
            textfieldBorder = Color.Transparent
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { SongViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            AIPlayerTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun LiveVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom
    ) {
        val transition = rememberInfiniteTransition(label = "visualizer")

        val height1 by if (isPlaying) {
            transition.animateFloat(
                initialValue = 0.2f, targetValue = 1.0f, animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse
                ), label = "h1"
            )
        } else {
            remember { mutableStateOf(0.2f) }
        }

        val height2 by if (isPlaying) {
            transition.animateFloat(
                initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse
                ), label = "h2"
            )
        } else {
            remember { mutableStateOf(0.3f) }
        }

        val height3 by if (isPlaying) {
            transition.animateFloat(
                initialValue = 0.1f, targetValue = 0.9f, animationSpec = infiniteRepeatable(
                    animation = tween(450, easing = LinearEasing), repeatMode = RepeatMode.Reverse
                ), label = "h3"
            )
        } else {
            remember { mutableStateOf(0.1f) }
        }

        Box(
            modifier = Modifier.width(3.dp).fillMaxHeight(height1)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier.width(3.dp).fillMaxHeight(height2)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier.width(3.dp).fillMaxHeight(height3)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun SongCover(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.MusicNote
) {
    val context = LocalContext.current
    var bitmap by remember(song.id) { mutableStateOf(song.getCachedCover()) }

    LaunchedEffect(song.id) {
        if (bitmap == null) {
            kotlinx.coroutines.delay(100)
            withContext(Dispatchers.IO) {
                bitmap = song.loadCover(context, 150)
            }
        }
    }

    Box(
        modifier = modifier.clip(shape).background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay visualizer or play icon
        if (isCurrent) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (bitmap == null) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = "Cover Placeholder",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PlaylistCover(
    firstSongId: Long?, currentAccent: AccentColor, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(firstSongId) { mutableStateOf(firstSongId?.let { getCachedCoverById(it) }) }

    LaunchedEffect(firstSongId) {
        if (firstSongId != null) {
            if (bitmap == null) {
                kotlinx.coroutines.delay(100)
                withContext(Dispatchers.IO) {
                    bitmap = loadCoverById(context, firstSongId, 150)
                }
            }
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(
            if (bitmap != null) Color(0xFF2C2C2E) else currentAccent.mainColor
        ), contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Playlist Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TopBar(
    activePlaylistId: Long?,
    activePlaylistName: String,
    onBackToPlaylists: () -> Unit,
    activeArtistName: String?,
    onBackToArtists: () -> Unit,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    isSearching: Boolean,
    onSearchStateChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showThemeDialog: () -> Unit,
    showSleepTimerDialog: () -> Unit,
    onScanClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onBlockedFoldersClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    onImportPlaylistsClick: () -> Unit,
    onExportPlaylistsClick: () -> Unit,
    appColors: AppColors,
    currentAccent: AccentColor,
    isDark: Boolean,
    scanState: ScanStatus,
    isCheckingUpdate: Boolean,
    onCheckUpdateClick: () -> Unit
) {
    val topBtnBg = if (isDark) Color(0x12FFFFFF) else Color(0x0C000000)

    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasActiveDetail = activePlaylistId != null || activeArtistName != null
        val detailTitle = when {
            activePlaylistId != null -> activePlaylistName
            activeArtistName != null -> if (activeArtistName.isBlank()) "未知歌手" else activeArtistName
            else -> ""
        }
        val onBackClick = when {
            activePlaylistId != null -> onBackToPlaylists
            activeArtistName != null -> onBackToArtists
            else -> ({})
        }

        if (hasActiveDetail) {
            IconButton(
                onClick = onBackClick, modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = appColors.textColorPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = detailTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textColorPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else if (isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(appColors.textfieldContainer), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onSearchStateChange(false)
                        onSearchQueryChange("")
                    }, modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "关闭搜索",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f).padding(end = 8.dp, top = 2.dp, bottom = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "搜索歌曲、歌手或专辑",
                            color = appColors.textColorSecondary.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = appColors.textColorPrimary, fontSize = 14.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(currentAccent.mainColor)
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") }, modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空",
                            tint = appColors.textColorSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val tab1Selected = currentScreen == Screen.Library
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onScreenChange(Screen.Library) }.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "全部歌曲",
                        fontSize = 17.sp,
                        fontWeight = if (tab1Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab1Selected) currentAccent.mainColor else appColors.textColorSecondary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.width(16.dp).height(2.5.dp).clip(CircleShape)
                            .background(if (tab1Selected) currentAccent.mainColor else Color.Transparent)
                    )
                }

                val tab2Selected = currentScreen == Screen.Playlists
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onScreenChange(Screen.Playlists) }.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "我的歌单",
                        fontSize = 17.sp,
                        fontWeight = if (tab2Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab2Selected) currentAccent.mainColor else appColors.textColorSecondary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.width(16.dp).height(2.5.dp).clip(CircleShape)
                            .background(if (tab2Selected) currentAccent.mainColor else Color.Transparent)
                    )
                }

                val tab3Selected = currentScreen == Screen.Artists
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onScreenChange(Screen.Artists) }.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "歌手",
                        fontSize = 17.sp,
                        fontWeight = if (tab3Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab3Selected) currentAccent.mainColor else appColors.textColorSecondary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.width(16.dp).height(2.5.dp).clip(CircleShape)
                            .background(if (tab3Selected) currentAccent.mainColor else Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { onSearchStateChange(true) }, modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = appColors.textColorPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (currentScreen == Screen.Playlists) {
                    IconButton(
                        onClick = onCreatePlaylistClick, modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "创建歌单",
                            tint = appColors.textColorPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                val isScanning = scanState is ScanStatus.Scanning

                Box {
                    IconButton(
                        onClick = { showMenu = true }, modifier = Modifier.size(36.dp)
                    ) {
                        if (isScanning) {
                            val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
                            val rotation = infiniteTransition.animateFloat(
                                initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart
                                ), label = "rotate"
                            ).value
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "正在扫描歌曲",
                                tint = currentAccent.mainColor,
                                modifier = Modifier.size(22.dp).rotate(rotation)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = appColors.textColorPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (showMenu) {
                        androidx.compose.ui.window.Popup(
                            onDismissRequest = { showMenu = false },
                            alignment = Alignment.TopEnd,
                            offset = androidx.compose.ui.unit.IntOffset(
                                0, with(androidx.compose.ui.platform.LocalDensity.current) { 40.dp.roundToPx() }),
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                        ) {
                            Box(
                                modifier = Modifier.width(130.dp).clip(RoundedCornerShape(4.dp))
                                    .background(appColors.surfaceColor).border(
                                        0.5.dp,
                                        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        RoundedCornerShape(4.dp)
                                    ).padding(vertical = 4.dp)
                            ) {
                                Column {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "主题设置", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showThemeDialog()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "屏蔽设置", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onBlockedFoldersClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    if (currentScreen == Screen.Playlists) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "导入歌单", color = appColors.textColorPrimary, fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onImportPlaylistsClick()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.FileUpload,
                                                    contentDescription = null,
                                                    tint = appColors.textColorPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.requiredHeight(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "导出歌单", color = appColors.textColorPrimary, fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onExportPlaylistsClick()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = null,
                                                    tint = appColors.textColorPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.requiredHeight(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                    }
                                    if (currentScreen == Screen.Library) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "排序方式", color = appColors.textColorPrimary, fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onSortOrderClick()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.List,
                                                    contentDescription = null,
                                                    tint = appColors.textColorPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.requiredHeight(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isScanning) "正在扫描..." else "扫描歌曲",
                                                    color = if (isScanning) appColors.textColorSecondary else appColors.textColorPrimary,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            enabled = !isScanning,
                                            onClick = {
                                                showMenu = false
                                                onScanClick()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = if (isScanning) appColors.textColorSecondary else appColors.textColorPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.requiredHeight(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "定时关闭", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showSleepTimerDialog()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isCheckingUpdate) "正在检查..." else "检查更新",
                                                color = if (isCheckingUpdate) appColors.textColorSecondary else appColors.textColorPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        enabled = !isCheckingUpdate,
                                        onClick = {
                                            showMenu = false
                                            onCheckUpdateClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = if (isCheckingUpdate) appColors.textColorSecondary else appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
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

    var currentScreen by remember { mutableStateOf(Screen.Library) }
    val pagerState = rememberPagerState(
        initialPage = when (currentScreen) {
            Screen.Library -> 0
            Screen.Playlists -> 1
            Screen.Artists -> 2
        },
        pageCount = { 3 }
    )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (!isProgrammaticScroll) {
            val targetScreen = when (pagerState.currentPage) {
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
        val targetPage = when (currentScreen) {
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
    var activePlaylistId by remember { mutableStateOf<Long?>(null) } // -1 for Favorites, -2 for Blacklist, positive for custom, null for none
    var activePlaylistName by remember { mutableStateOf("") }
    var activeArtistName by remember { mutableStateOf<String?>(null) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var previousPlaylistId by remember { mutableStateOf<Long?>(null) }
    var lastActivePlaylistId by remember { mutableStateOf<Long?>(null) }
    if (activePlaylistId != null) {
        lastActivePlaylistId = activePlaylistId
    }
    var lastActiveArtistName by remember { mutableStateOf<String?>(null) }
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
                val url = URL("https://api.github.com/repos/X-X-X-X-X-X-X-X-X-X-X-X-X/AI-Player/releases/latest")
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
                                Toast.makeText(context, "当前已是最新版本 (v$currentVersionName)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        Toast.makeText(context, "检查更新失败，响应码: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        checkForUpdates(showToastIfLatest = false)
    }

    var showFullPlayer by remember { mutableStateOf(false) }

    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
    val playerOffsetY = remember { androidx.compose.animation.core.Animatable(screenHeight) }
    var hasOpenedPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(showFullPlayer) {
        if (showFullPlayer) {
            hasOpenedPlayer = true
            playerOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            launch {
                playerOffsetY.animateTo(
                    targetValue = screenHeight,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
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

    val miniPlayerDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = {
                // Ensure we start with showFullPlayer = false, but the player is composed
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    playerOffsetY.snapTo((playerOffsetY.value + dragAmount).coerceIn(0f, screenHeight))
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    if (playerOffsetY.value < screenHeight * 0.85f) {
                        showFullPlayer = true
                    } else {
                        playerOffsetY.animateTo(screenHeight, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                    }
                }
            },
            onDragCancel = {
                coroutineScope.launch {
                    playerOffsetY.animateTo(screenHeight, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                }
            }
        )
    }

    val playerDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    playerOffsetY.snapTo((playerOffsetY.value + dragAmount).coerceIn(0f, screenHeight))
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    if (playerOffsetY.value > screenHeight * 0.15f) {
                        showFullPlayer = false
                    } else {
                        playerOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                    }
                }
            },
            onDragCancel = {
                coroutineScope.launch {
                    if (showFullPlayer) {
                        playerOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                    } else {
                        playerOffsetY.animateTo(screenHeight, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                    }
                }
            }
        )
    }

    val exportPlaylistsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"), onResult = { uri ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val jsonString = viewModel.exportPlaylistsJson()
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                        }
                        val displayPath = try {
                            var fileName: String? = null
                            if (it.scheme == "content") {
                                context.contentResolver.query(
                                    it, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
                                )?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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
                            Toast.makeText(context, "歌单导出成功，已保存至: $displayPath", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })

    var importResult by remember { mutableStateOf<cn.xuexc.ai_player.ui.ImportResult?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val importPlaylistsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(), onResult = { uri ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val content = context.contentResolver.openInputStream(it)?.use { inputStream ->
                            inputStream.bufferedReader().use { reader -> reader.readText() }
                        }
                        if (content != null) {
                            val result = viewModel.importPlaylistsJson(content)
                            withContext(Dispatchers.Main) {
                                importResult = result
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                importError = "读取文件失败"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            importError = "导入失败: ${e.message}"
                        }
                    }
                }
            }
        })

    androidx.activity.compose.BackHandler(enabled = showFullPlayer || activeArtistName != null || activePlaylistId != null) {
        if (showFullPlayer) {
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

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activePlaylistId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
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
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activeListState by remember(currentScreen, activePlaylistId, activeArtistName) {
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

    val isAtBottom by remember(activeListState) {
        derivedStateOf {
            val state = activeListState ?: return@derivedStateOf false
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index == layoutInfo.totalItemsCount - 1 && (lastVisibleItem.offset + lastVisibleItem.size) <= (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding + 10)
            }
        }
    }
    var showBlockedFoldersDialog by remember { mutableStateOf(false) }
    var showSortOrderDialog by remember { mutableStateOf(false) }
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isSortAscending by viewModel.isSortAscending.collectAsState()
    val isTitleSort = sortOrder == SortOrder.BY_TITLE
    val blockedFolders by viewModel.blockedFolders.collectAsState()
    val detectedFolders by viewModel.detectedFolders.collectAsState()

    val themeName by viewModel.themeName.collectAsState()
    val currentAccent = remember(themeName) {
        AccentColor.values().firstOrNull { it.id == themeName } ?: AccentColor.TitaniumGray
    }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appColors = remember(currentAccent, isDarkMode) {
        getAppColors(currentAccent, isDarkMode)
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkMode
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

    // Playlists state
    val playlists by viewModel.playlists.collectAsState()
    val isSongsLoaded by viewModel.isSongsLoaded.collectAsState()
    val isPlaylistsLoaded by viewModel.isPlaylistsLoaded.collectAsState()
    val favoriteSongsCount by viewModel.favoriteSongsCount.collectAsState()
    val blacklistSongsCount by viewModel.blacklistSongsCount.collectAsState()
    val playlistSongs by viewModel.currentPlaylistSongs.collectAsState()

    val showLocateSong by remember(
        currentSong, currentScreen, activePlaylistId, activeArtistName, songs, playlistSongs
    ) {
        derivedStateOf {
            val curSong = currentSong ?: return@derivedStateOf false
            when {
                activePlaylistId != null -> playlistSongs.any { it.id == curSong.id }
                activeArtistName != null -> songs.any { it.artist == activeArtistName && it.id == curSong.id }
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


    // Determine correct permission based on Android version
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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
                                songsDisplayList.indexOfFirst { it is SongItemWithLetter && it.song.id == curSong.id }
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
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().background(appColors.mainBackground).padding(innerPadding)
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
                    onImportPlaylistsClick = { importPlaylistsLauncher.launch(arrayOf("application/json")) },
                    onExportPlaylistsClick = { exportPlaylistsLauncher.launch("ai_playlists.json") },
                    appColors = appColors,
                    currentAccent = currentAccent,
                    isDark = isDarkMode,
                    scanState = scanState,
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdateClick = { checkForUpdates(true) }
                )

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activePlaylistId != null,
                        enter = androidx.compose.animation.EnterTransition.None,
                        exit = androidx.compose.animation.ExitTransition.None
                    ) {
                        val currentPlaylistId = lastActivePlaylistId
                        if (currentPlaylistId != null) {
                            // PLAYLIST DETAILS PAGE
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                            ) {
                                // 随机播放控制栏
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                                            val shuffled = playlistSongs.shuffled()
                                            if (shuffled.isNotEmpty()) {
                                                viewModel.setPlaybackQueue(shuffled)
                                                while (viewModel.playMode.value != PlayMode.Shuffle) {
                                                    viewModel.togglePlayMode()
                                                }
                                                viewModel.playSong(context, shuffled[0])
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "随机播放",
                                            tint = currentAccent.mainColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "随机播放",
                                            color = currentAccent.mainColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "共 ${playlistSongs.size} 首歌曲",
                                        color = appColors.textColorSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (playlistSongs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1.0f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (currentPlaylistId == -2L) "遗忘的沙漏为空" else "歌单为空",
                                            color = appColors.textColorSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = playlistSongsListState,
                                        modifier = Modifier.fillMaxWidth().weight(1.0f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        items(playlistSongs, key = { it.id }) { song ->
                                            val isCurrent = currentSong?.id == song.id
                                            SongItemCard(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && isPlaying,
                                                onDelete = {
                                                    songToDelete = song

                                                },
                                                appColors = appColors,
                                                onNavigateToArtist = { artist ->
                                                    previousScreen = currentScreen
                                                    previousPlaylistId = currentPlaylistId
                                                    currentScreen = Screen.Artists
                                                    activeArtistName = artist
                                                    activePlaylistId = null
                                                },
                                                onFavoriteToggle = if (currentPlaylistId == -2L) null else {
                                                    {
                                                        viewModel.toggleFavorite(song)
                                                    }
                                                },
                                                onBlacklistToggle = if (currentPlaylistId == -2L) null else {
                                                    {
                                                        viewModel.addToBlacklist(context, song)
                                                        if (currentPlaylistId == -1L) {
                                                            viewModel.loadFavoriteSongs()
                                                        } else {
                                                            viewModel.loadSongsInPlaylist(currentPlaylistId)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setPlaybackQueue(playlistSongs)
                                                    viewModel.playSong(context, song)
                                                },
                                                customActionIcon = when {
                                                    currentPlaylistId == -2L -> Icons.Default.Refresh // Refresh icon to restore from blacklist
                                                    currentPlaylistId > 0L -> Icons.Default.Clear // Cross to remove from playlist
                                                    else -> null
                                                },
                                                onCustomAction = when {
                                                    currentPlaylistId == -2L -> {
                                                        {
                                                            viewModel.removeFromBlacklist(song)
                                                            Toast.makeText(
                                                                context, "已从遗忘的沙漏恢复", Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }

                                                    currentPlaylistId > 0L -> {
                                                        {
                                                            songToRemoveFromPlaylist = currentPlaylistId to song
                                                        }
                                                    }

                                                    else -> null
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeArtistName != null,
                        enter = androidx.compose.animation.EnterTransition.None,
                        exit = androidx.compose.animation.ExitTransition.None
                    ) {
                        val currentArtistName = lastActiveArtistName
                        if (currentArtistName != null) {
                            // ARTIST SONGS DETAILS PAGE
                            val artistSongs = remember(songs, currentArtistName) {
                                songs.filter { it.artist == currentArtistName }
                            }
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                            ) {
                                // 随机播放控制栏
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                                            val shuffled = artistSongs.shuffled()
                                            if (shuffled.isNotEmpty()) {
                                                viewModel.setPlaybackQueue(shuffled)
                                                while (viewModel.playMode.value != PlayMode.Shuffle) {
                                                    viewModel.togglePlayMode()
                                                }
                                                viewModel.playSong(context, shuffled[0])
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "随机播放",
                                            tint = currentAccent.mainColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "随机播放",
                                            color = currentAccent.mainColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "共 ${artistSongs.size} 首歌曲",
                                        color = appColors.textColorSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (artistSongs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1.0f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "暂无该歌手的歌曲",
                                            color = appColors.textColorSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = artistSongsDetailListState,
                                        modifier = Modifier.fillMaxWidth().weight(1.0f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        items(artistSongs, key = { it.id }) { song ->
                                            val isCurrent = currentSong?.id == song.id
                                            SongItemCard(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && isPlaying,
                                                onDelete = {
                                                    songToDelete = song

                                                },
                                                appColors = appColors,
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
                                                    viewModel.setPlaybackQueue(artistSongs)
                                                    viewModel.playSong(context, song)
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activePlaylistId == null && activeArtistName == null) {
                        // TAB PAGES
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val screen = when (page) {
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
                                    val alphabet = remember { ('A'..'Z').toList() + '#' }
                                    var alphabetHeight by remember { mutableStateOf(1f) }

                                    val firstVisibleIndex by remember { derivedStateOf { songListState.firstVisibleItemIndex } }
                                    LaunchedEffect(firstVisibleIndex, isTitleSort) {
                                        if (isTitleSort && !showLetterIndicator && songsDisplayList.isNotEmpty()) {
                                            val visibleItem = songsDisplayList.getOrNull(firstVisibleIndex)
                                            val currentLetter = when (visibleItem) {
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

                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            // Scan Status Card
                                            var lastNonIdleStatus by remember { mutableStateOf<ScanStatus>(ScanStatus.Idle) }
                                            LaunchedEffect(scanState) {
                                                if (scanState !is ScanStatus.Idle) {
                                                    lastNonIdleStatus = scanState
                                                }
                                            }

                                            AnimatedVisibility(
                                                visible = scanState !is ScanStatus.Idle, exit = slideOutVertically(
                                                    targetOffsetY = { -it }, animationSpec = tween(
                                                        durationMillis = 350,
                                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                    )
                                                ) + shrinkVertically(
                                                    animationSpec = tween(
                                                        durationMillis = 350,
                                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                    )
                                                ) + fadeOut(
                                                    animationSpec = tween(durationMillis = 200)
                                                )
                                            ) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    shape = RoundedCornerShape(20.dp),
                                                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        when (val status = lastNonIdleStatus) {
                                                            is ScanStatus.Scanning -> {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    color = currentAccent.mainColor,
                                                                    strokeWidth = 2.dp
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Text(
                                                                    "正在扫描本地音频文件...",
                                                                    color = appColors.textColorPrimary,
                                                                    fontSize = 14.sp
                                                                )
                                                            }

                                                            is ScanStatus.Success -> {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Success",
                                                                    tint = currentAccent.mainColor,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Column(modifier = Modifier.weight(1.0f)) {
                                                                    Text(
                                                                        "扫描完成！",
                                                                        color = appColors.textColorPrimary,
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    Text(
                                                                        "共发现 ${status.count} 首歌曲",
                                                                        color = appColors.textColorSecondary,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                                TextButton(onClick = { viewModel.resetScanState() }) {
                                                                    Text("确定", color = currentAccent.mainColor)
                                                                }
                                                            }

                                                            is ScanStatus.Error -> {
                                                                Icon(
                                                                    imageVector = Icons.Default.Warning,
                                                                    contentDescription = "Error",
                                                                    tint = Color.Red,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Column(modifier = Modifier.weight(1.0f)) {
                                                                    Text(
                                                                        "扫描出错",
                                                                        color = appColors.textColorPrimary,
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    Text(
                                                                        status.message,
                                                                        color = appColors.textColorSecondary,
                                                                        fontSize = 12.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                TextButton(onClick = { viewModel.resetScanState() }) {
                                                                    Text("重试", color = currentAccent.mainColor)
                                                                }
                                                            }

                                                            else -> {}
                                                        }
                                                    }
                                                }
                                            }

                                            // Songs list or Empty state
                                            if (songs.isEmpty()) {
                                                if (isSongsLoaded) {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                            .padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.size(80.dp)
                                                                    .clip(RoundedCornerShape(40.dp))
                                                                    .background(appColors.textfieldContainer),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlayArrow,
                                                                    contentDescription = "No Music",
                                                                    tint = appColors.textColorSecondary.copy(alpha = 0.5f),
                                                                    modifier = Modifier.size(40.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Text(
                                                                text = "未扫描到本地歌曲",
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = appColors.textColorPrimary
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "点击上方刷新按钮或下方按钮开始扫描",
                                                                fontSize = 12.sp,
                                                                color = appColors.textColorSecondary
                                                            )
                                                            Spacer(modifier = Modifier.height(24.dp))
                                                            Button(
                                                                onClick = { checkAndStartScan() },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = currentAccent.mainColor
                                                                ),
                                                                shape = RoundedCornerShape(24.dp),
                                                                modifier = Modifier.width(160.dp).height(48.dp)
                                                            ) {
                                                                Text(
                                                                    "扫描歌曲",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                    )
                                                }
                                            } else {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                ) {
                                                    // 随机播放控制栏
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                                                .clickable {
                                                                    val shuffled = songs.shuffled()
                                                                    if (shuffled.isNotEmpty()) {
                                                                        viewModel.setPlaybackQueue(shuffled)
                                                                        while (viewModel.playMode.value != PlayMode.Shuffle) {
                                                                            viewModel.togglePlayMode()
                                                                        }
                                                                        viewModel.playSong(context, shuffled[0])
                                                                    }
                                                                }.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Shuffle,
                                                                contentDescription = "随机播放",
                                                                tint = currentAccent.mainColor,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "随机播放",
                                                                color = currentAccent.mainColor,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Text(
                                                            text = "共 ${songs.size} 首歌曲",
                                                            color = appColors.textColorSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                    ) {
                                                        LazyColumn(
                                                            state = songListState,
                                                            modifier = Modifier.fillMaxSize().padding(
                                                                start = 16.dp, end = if (isTitleSort) 36.dp else 16.dp
                                                            ),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                                            contentPadding = PaddingValues(bottom = 100.dp)
                                                        ) {
                                                            if (isTitleSort) {
                                                                itemsIndexed(songsDisplayList) { index, item ->
                                                                    when (item) {
                                                                        is Char -> {
                                                                            CharacterHeader(
                                                                                letter = item, appColors = appColors
                                                                            )
                                                                        }

                                                                        is SongItemWithLetter -> {
                                                                            val song = item.song
                                                                            val isCurrent = currentSong?.id == song.id
                                                                            SongItemCard(
                                                                                song = song,
                                                                                isCurrent = isCurrent,
                                                                                isPlaying = isCurrent && isPlaying,
                                                                                onDelete = {
                                                                                    songToDelete = song

                                                                                },
                                                                                appColors = appColors,
                                                                                onNavigateToArtist = { artist ->
                                                                                    previousScreen = currentScreen
                                                                                    previousPlaylistId =
                                                                                        activePlaylistId
                                                                                    currentScreen = Screen.Artists
                                                                                    activeArtistName = artist
                                                                                    activePlaylistId = null
                                                                                },
                                                                                onFavoriteToggle = {
                                                                                    viewModel.toggleFavorite(
                                                                                        song
                                                                                    )
                                                                                },
                                                                                onBlacklistToggle = {
                                                                                    viewModel.addToBlacklist(
                                                                                        context, song
                                                                                    )
                                                                                },
                                                                                onClick = {
                                                                                    viewModel.setPlaybackQueue(songs)
                                                                                    viewModel.playSong(context, song)
                                                                                },
                                                                                customActionIcon = Icons.Default.Add,
                                                                                onCustomAction = {
                                                                                    selectedSongForAddToPlaylist = song
                                                                                    showAddToPlaylistDialog = true
                                                                                })
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                items(songs, key = { it.id }) { song ->
                                                                    val isCurrent = currentSong?.id == song.id
                                                                    SongItemCard(
                                                                        song = song,
                                                                        isCurrent = isCurrent,
                                                                        isPlaying = isCurrent && isPlaying,
                                                                        onDelete = {
                                                                            songToDelete = song

                                                                        },
                                                                        appColors = appColors,
                                                                        onNavigateToArtist = { artist ->
                                                                            previousScreen = currentScreen
                                                                            previousPlaylistId = activePlaylistId
                                                                            currentScreen = Screen.Artists
                                                                            activeArtistName = artist
                                                                            activePlaylistId = null
                                                                        },
                                                                        onFavoriteToggle = {
                                                                            viewModel.toggleFavorite(
                                                                                song
                                                                            )
                                                                        },
                                                                        onBlacklistToggle = {
                                                                            viewModel.addToBlacklist(
                                                                                context, song
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            viewModel.setPlaybackQueue(songs)
                                                                            viewModel.playSong(context, song)
                                                                        },
                                                                        customActionIcon = Icons.Default.Add,
                                                                        onCustomAction = {
                                                                            selectedSongForAddToPlaylist = song
                                                                            showAddToPlaylistDialog = true
                                                                        })
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 右侧 A-Z 定位条
                                        if (songs.isNotEmpty() && isTitleSort) {
                                            Column(
                                                modifier = Modifier.align(Alignment.CenterEnd).width(16.dp)
                                                    .padding(end = 4.dp)
                                                    .onGloballyPositioned { alphabetHeight = it.size.height.toFloat() }
                                                    .pointerInput(alphabet) {
                                                        detectTapGestures(
                                                            onPress = { offset ->
                                                                val itemHeight = alphabetHeight / alphabet.size
                                                                val index = (offset.y / itemHeight).toInt()
                                                                    .coerceIn(0, alphabet.lastIndex)
                                                                val letter = alphabet[index]
                                                                selectedLetter = letter
                                                                showLetterIndicator = true
                                                                scrollToLetter(letter)
                                                                tryAwaitRelease()
                                                                showLetterIndicator = false
                                                            })
                                                    }.pointerInput(alphabet) {
                                                        detectDragGestures(onDragStart = { offset ->
                                                            val itemHeight = alphabetHeight / alphabet.size
                                                            val index = (offset.y / itemHeight).toInt()
                                                                .coerceIn(0, alphabet.lastIndex)
                                                            val letter = alphabet[index]
                                                            selectedLetter = letter
                                                            showLetterIndicator = true
                                                            scrollToLetter(letter)
                                                        }, onDrag = { change, _ ->
                                                            val itemHeight = alphabetHeight / alphabet.size
                                                            val index = (change.position.y / itemHeight).toInt()
                                                                .coerceIn(0, alphabet.lastIndex)
                                                            val letter = alphabet[index]
                                                            if (selectedLetter != letter) {
                                                                selectedLetter = letter
                                                                scrollToLetter(letter)
                                                            }
                                                        }, onDragEnd = {
                                                            showLetterIndicator = false
                                                        }, onDragCancel = {
                                                            showLetterIndicator = false
                                                        })
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                alphabet.forEach { letter ->
                                                    val isSelected = selectedLetter == letter
                                                    Text(
                                                        text = letter.toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) currentAccent.mainColor else appColors.textColorSecondary.copy(
                                                            alpha = 0.6f
                                                        ),
                                                        modifier = Modifier.padding(vertical = 0.5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (songs.isNotEmpty() && isTitleSort) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = showLetterIndicator && selectedLetter != null,
                                                enter = fadeIn(animationSpec = tween(100)),
                                                exit = fadeOut(animationSpec = tween(150)),
                                                modifier = Modifier.align(Alignment.Center)
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(80.dp).clip(CircleShape)
                                                        .background(currentAccent.mainColor.copy(alpha = 0.85f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = selectedLetter?.toString() ?: "",
                                                        color = Color.White,
                                                        fontSize = 38.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Screen.Playlists -> {
                                    // PLAYLISTS TAB
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                                    ) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().weight(1.0f),
                                            contentPadding = PaddingValues(bottom = 100.dp)
                                        ) {
                                            // 1. Built-in Favorite & Blacklist Playlists Panel (Side-by-side Row)
                                            item {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Favorite Card
                                                    val favBgColor =
                                                        if (isDarkMode) Color(0xFF2E1C1F) else Color(0xFFFFF0F2)
                                                    Card(
                                                        modifier = Modifier.weight(1f).height(68.dp).clickable {
                                                            activePlaylistId = -1L
                                                            activePlaylistName = "喜欢歌单"
                                                            viewModel.loadFavoriteSongs()
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize().background(favBgColor)
                                                                .padding(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize(),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.size(36.dp)
                                                                        .clip(RoundedCornerShape(8.dp)).background(
                                                                            if (isDarkMode) Color.White.copy(
                                                                                alpha = 0.15f
                                                                            ) else Color.White
                                                                        ), contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Favorite,
                                                                        contentDescription = "喜欢",
                                                                        tint = Color(0xFFE06C75),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column(modifier = Modifier.weight(1.0f)) {
                                                                    Text(
                                                                        text = "喜欢歌单",
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = appColors.textColorPrimary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Spacer(modifier = Modifier.height(1.dp))
                                                                    Text(
                                                                        text = "$favoriteSongsCount 首歌曲",
                                                                        fontSize = 11.sp,
                                                                        color = appColors.textColorSecondary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Forgotten Hourglass Card
                                                    val blackBgColor =
                                                        if (isDarkMode) Color(0xFF202022) else Color(0xFFEBEBEF)
                                                    Card(
                                                        modifier = Modifier.weight(1f).height(68.dp).clickable {
                                                            activePlaylistId = -2L
                                                            activePlaylistName = "遗忘的沙漏"
                                                            viewModel.loadBlacklistSongs()
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize().background(blackBgColor)
                                                                .padding(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize(),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.size(36.dp)
                                                                        .clip(RoundedCornerShape(8.dp)).background(
                                                                            if (isDarkMode) Color.White.copy(
                                                                                alpha = 0.1f
                                                                            ) else Color(0x0F000000)
                                                                        ), contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.HourglassEmpty,
                                                                        contentDescription = "遗忘的沙漏",
                                                                        tint = if (isDarkMode) Color(0xFFE5C07B).copy(
                                                                            alpha = 0.8f
                                                                        ) else Color(0xFFE5C07B),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column(modifier = Modifier.weight(1.0f)) {
                                                                    Text(
                                                                        text = "遗忘的沙漏",
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = appColors.textColorPrimary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Spacer(modifier = Modifier.height(1.dp))
                                                                    Text(
                                                                        text = "$blacklistSongsCount 首歌曲",
                                                                        fontSize = 11.sp,
                                                                        color = appColors.textColorSecondary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
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
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }

                                                items(playlists, key = { it.id }) { playlist ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            activePlaylistId = playlist.id
                                                            activePlaylistName = playlist.name
                                                            viewModel.loadSongsInPlaylist(playlist.id)
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Cover placeholder
                                                            PlaylistCover(
                                                                firstSongId = playlist.firstSongId,
                                                                currentAccent = currentAccent,
                                                                modifier = Modifier.size(36.dp)
                                                            )

                                                            Spacer(modifier = Modifier.width(8.dp))

                                                            Column(modifier = Modifier.weight(1.0f)) {
                                                                Text(
                                                                    text = playlist.name,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = appColors.textColorPrimary
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = "${playlist.songCount} 首歌曲",
                                                                    fontSize = 11.sp,
                                                                    color = appColors.textColorSecondary
                                                                )
                                                            }

                                                            // Delete Playlist Action
                                                            IconButton(
                                                                onClick = { playlistToDelete = playlist },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "删除歌单",
                                                                    tint = appColors.textColorSecondary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (isPlaylistsLoaded) {
                                                    item {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "暂无自定义歌单\n点击右上角 '+' 按钮创建",
                                                                color = appColors.textColorSecondary,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    item {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().height(120.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Screen.Artists -> {
                                    // ARTISTS TAB

                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (artistDisplayList.isEmpty()) {
                                            if (isSongsLoaded) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                        .padding(horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "暂无歌手信息。请先扫描歌曲。",
                                                        color = appColors.textColorSecondary,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().weight(1.0f)
                                                )
                                            }
                                        } else {
                                            var selectedLetter by remember { mutableStateOf<Char?>(null) }
                                            var showLetterIndicator by remember { mutableStateOf(false) }
                                            val alphabet = remember { ('A'..'Z').toList() + '#' }
                                            var alphabetHeight by remember { mutableStateOf(1f) }

                                            val firstVisibleIndex by remember { derivedStateOf { artistListState.firstVisibleItemIndex } }
                                            LaunchedEffect(firstVisibleIndex) {
                                                if (!showLetterIndicator && artistDisplayList.isNotEmpty()) {
                                                    val visibleItem = artistDisplayList.getOrNull(firstVisibleIndex)
                                                    val currentLetter = when (visibleItem) {
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
                                                val targetIndex = getTargetIndex(letter, artistIndexMap)
                                                if (targetIndex != -1) {
                                                    coroutineScope.launch {
                                                        artistListState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier.fillMaxWidth().weight(1.0f)
                                            ) {
                                                // 歌手列表
                                                LazyColumn(
                                                    state = artistListState,
                                                    modifier = Modifier.fillMaxSize()
                                                        .padding(start = 16.dp, end = 36.dp), // 右边留出 36.dp 给侧边栏
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    contentPadding = PaddingValues(bottom = 100.dp)
                                                ) {
                                                    itemsIndexed(artistDisplayList) { index, item ->
                                                        when (item) {
                                                            is Char -> {
                                                                CharacterHeader(letter = item, appColors = appColors)
                                                            }

                                                            is ArtistItem -> {
                                                                Card(
                                                                    modifier = Modifier.fillMaxWidth().clickable {
                                                                        activeArtistName = item.artistKey
                                                                    },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground)
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth().padding(
                                                                            horizontal = 10.dp, vertical = 6.dp
                                                                        ),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        // Artist avatar using first song cover
                                                                        val firstSong = item.songs.firstOrNull()
                                                                        if (firstSong != null) {
                                                                            SongCover(
                                                                                song = firstSong,
                                                                                isCurrent = false,
                                                                                isPlaying = false,
                                                                                modifier = Modifier.size(36.dp),
                                                                                shape = CircleShape,
                                                                                fallbackIcon = Icons.Default.Person
                                                                            )
                                                                        } else {
                                                                            Box(
                                                                                modifier = Modifier.size(36.dp)
                                                                                    .clip(CircleShape).background(
                                                                                        currentAccent.mainColor.copy(
                                                                                            alpha = 0.15f
                                                                                        )
                                                                                    ),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Person,
                                                                                    contentDescription = null,
                                                                                    tint = currentAccent.mainColor,
                                                                                    modifier = Modifier.size(18.dp)
                                                                                )
                                                                            }
                                                                        }

                                                                        Spacer(modifier = Modifier.width(10.dp))

                                                                        Column(modifier = Modifier.weight(1.0f)) {
                                                                            Text(
                                                                                text = item.displayName,
                                                                                fontSize = 15.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = appColors.textColorPrimary,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                            Spacer(modifier = Modifier.height(2.dp))
                                                                            Text(
                                                                                text = "${item.songs.size} 首歌曲",
                                                                                fontSize = 12.sp,
                                                                                color = appColors.textColorSecondary
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // 右侧 A-Z 定位条
                                                Column(
                                                    modifier = Modifier.align(Alignment.CenterEnd).width(16.dp)
                                                        .padding(end = 4.dp).onGloballyPositioned {
                                                            alphabetHeight = it.size.height.toFloat()
                                                        }.pointerInput(alphabet) {
                                                            detectTapGestures(
                                                                onPress = { offset ->
                                                                    val itemHeight = alphabetHeight / alphabet.size
                                                                    val index = (offset.y / itemHeight).toInt()
                                                                        .coerceIn(0, alphabet.lastIndex)
                                                                    val letter = alphabet[index]
                                                                    selectedLetter = letter
                                                                    showLetterIndicator = true
                                                                    scrollToLetter(letter)
                                                                    tryAwaitRelease()
                                                                    showLetterIndicator = false
                                                                })
                                                        }.pointerInput(alphabet) {
                                                            detectDragGestures(onDragStart = { offset ->
                                                                val itemHeight = alphabetHeight / alphabet.size
                                                                val index = (offset.y / itemHeight).toInt()
                                                                    .coerceIn(0, alphabet.lastIndex)
                                                                val letter = alphabet[index]
                                                                selectedLetter = letter
                                                                showLetterIndicator = true
                                                                scrollToLetter(letter)
                                                            }, onDrag = { change, _ ->
                                                                val itemHeight = alphabetHeight / alphabet.size
                                                                val index = (change.position.y / itemHeight).toInt()
                                                                    .coerceIn(0, alphabet.lastIndex)
                                                                val letter = alphabet[index]
                                                                if (selectedLetter != letter) {
                                                                    selectedLetter = letter
                                                                    scrollToLetter(letter)
                                                                }
                                                            }, onDragEnd = {
                                                                showLetterIndicator = false
                                                            }, onDragCancel = {
                                                                showLetterIndicator = false
                                                            })
                                                        },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    alphabet.forEach { letter ->
                                                        val isSelected = selectedLetter == letter
                                                        Text(
                                                            text = letter.toString(),
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) currentAccent.mainColor else appColors.textColorSecondary.copy(
                                                                alpha = 0.6f
                                                            ),
                                                            modifier = Modifier.padding(vertical = 0.5.dp)
                                                        )
                                                    }
                                                }

                                                // 屏幕正中央大字字母提示气泡 (Bubble Indicator)
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = showLetterIndicator && selectedLetter != null,
                                                    enter = fadeIn(animationSpec = tween(100)),
                                                    exit = fadeOut(animationSpec = tween(150)),
                                                    modifier = Modifier.align(Alignment.Center)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(80.dp).clip(CircleShape)
                                                            .background(currentAccent.mainColor.copy(alpha = 0.85f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = selectedLetter?.toString() ?: "",
                                                            color = Color.White,
                                                            fontSize = 38.sp,
                                                            fontWeight = FontWeight.Bold
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

                    // Bottom Fixed Docked Mini Player
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentSong != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        currentSong?.let { song ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { showFullPlayer = true }
                                    .then(miniPlayerDragModifier),
                                shape = RoundedCornerShape(0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF161619) else Color(0xFFFFFFFF)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(68.dp)
                                    ) {
                                        // Top subtle divider line
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(1.dp)
                                                .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0D000000))
                                                .align(Alignment.TopCenter)
                                        )

                                        // Top progress line (docked on top of the divider)
                                        val progress = if (song.duration > 0) {
                                            playbackProgress.toFloat() / song.duration.toFloat()
                                        } else {
                                            0f
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.5.dp)
                                                .background(currentAccent.mainColor).align(Alignment.TopStart)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(start = 26.dp, end = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Small Cover Art
                                            SongCover(
                                                song = song,
                                                isCurrent = false,
                                                isPlaying = false,
                                                modifier = Modifier.size(42.dp)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Meta details
                                            Column(modifier = Modifier.weight(1.0f)) {
                                                Text(
                                                    text = song.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = appColors.textColorPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val quality = song.getQualityType()
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (quality != QualityType.SQ) {
                                                        QualityBadge(quality = quality, isDarkMode = isDarkMode)
                                                    }
                                                    Text(
                                                        text = song.artist,
                                                        fontSize = 11.sp,
                                                        color = appColors.textColorSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Play / Pause Button
                                            IconButton(
                                                onClick = {
                                                    if (isPlaying) {
                                                        viewModel.pauseSong()
                                                    } else {
                                                        viewModel.resumeSong()
                                                    }
                                                }, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    tint = currentAccent.mainColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.navigationBarsPadding())
                                }
                            }
                        }
                    }

                    val backToTopBottomPadding by animateDpAsState(
                        targetValue = if (currentSong != null) 84.dp else 16.dp, label = "back_to_top_padding"
                    )

                    val locateInteractionSource = remember { MutableInteractionSource() }
                    val isLocatePressed by locateInteractionSource.collectIsPressedAsState()
                    val locateAlpha by animateFloatAsState(
                        targetValue = if (isLocatePressed) 1.0f else 0.92f, label = "locate_alpha"
                    )
                    val locateScale by animateFloatAsState(
                        targetValue = if (isLocatePressed) 0.86f else 1.0f, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
                        ), label = "locate_scale"
                    )


                    val topInteractionSource = remember { MutableInteractionSource() }
                    val isTopPressed by topInteractionSource.collectIsPressedAsState()
                    val topAlpha by animateFloatAsState(
                        targetValue = if (isTopPressed) 1.0f else 0.92f, label = "top_alpha"
                    )
                    val topScale by animateFloatAsState(
                        targetValue = if (isTopPressed) 0.86f else 1.0f, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
                        ), label = "top_scale"
                    )


                    val locateOffset by animateDpAsState(
                        targetValue = if (showBackToTop) 52.dp else 0.dp, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
                        ), label = "locate_offset"
                    )

                    val buttonBorder = Modifier.border(
                        width = 1.dp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        shape = CircleShape
                    )



                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = backToTopBottomPadding).navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // 1. 定位当前歌曲按钮
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showLocateSong && !isAtBottom,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier = Modifier.padding(bottom = if (locateOffset < 0.dp) 0.dp else locateOffset)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).graphicsLayer {
                                    alpha = locateAlpha
                                    scaleX = locateScale
                                    scaleY = locateScale
                                }.clip(CircleShape).background(currentAccent.mainColor).then(buttonBorder).clickable(
                                    interactionSource = locateInteractionSource,
                                    indication = androidx.compose.material3.ripple(color = Color.White),
                                    onClick = {
                                        locateCurrentSong()
                                    }), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "定位当前歌曲",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        if (isLocatePressed) Color.White.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                )
                            }
                        }

                        // 2. 回到顶部按钮
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBackToTop && !isAtBottom,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).graphicsLayer {
                                    alpha = topAlpha
                                    scaleX = topScale
                                    scaleY = topScale
                                }.clip(CircleShape).background(currentAccent.mainColor).then(buttonBorder).clickable(
                                    interactionSource = topInteractionSource,
                                    indication = androidx.compose.material3.ripple(color = Color.White),
                                    onClick = {
                                        coroutineScope.launch {
                                            activeListState?.scrollToItem(0)
                                        }
                                    }), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "回到顶部",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        if (isTopPressed) Color.White.copy(alpha = 0.15f) else Color.Transparent
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
        val dialogBg = appColors.surfaceColor
        val itemBg = if (isDarkMode) Color(0x0CFFFFFF) else Color(0x06000000)
        val currentSortOrder by viewModel.sortOrder.collectAsState()

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSortOrderDialog = false }) {
            androidx.compose.material3.Surface(
                shape = DialogShape,
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "选择排序方式",
                            color = currentAccent.mainColor,
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing)
                        ) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (isSortAscending) currentAccent.mainColor.copy(alpha = 0.15f) else itemBg)
                                    .clickable { viewModel.setSortAscending(true) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "正序",
                                    color = if (isSortAscending) currentAccent.mainColor else appColors.textColorPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (!isSortAscending) currentAccent.mainColor.copy(alpha = 0.15f) else itemBg)
                                    .clickable { viewModel.setSortAscending(false) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "倒序",
                                    color = if (!isSortAscending) currentAccent.mainColor else appColors.textColorPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SortOrder.values().forEach { order ->
                                val isSelected = currentSortOrder == order
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) currentAccent.mainColor.copy(alpha = 0.15f) else itemBg)
                                        .clickable {
                                            viewModel.setSortOrder(order)
                                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = order.displayName,
                                        color = if (isSelected) currentAccent.mainColor else appColors.textColorPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = currentAccent.mainColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showSortOrderDialog = false },
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = appColors.textColorSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // 0. Blocked Folders Management Dialog
    if (showBlockedFoldersDialog) {
        val dialogBg = appColors.surfaceColor
        val itemBg = if (isDarkMode) Color(0x0CFFFFFF) else Color(0x06000000)

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBlockedFoldersDialog = false }) {
            androidx.compose.material3.Surface(
                shape = DialogShape,
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {

                        // 2. Section: 已屏蔽的文件夹
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(end = 32.dp) // 留出空间给右上角关闭按钮
                            ) {
                                Text(
                                    text = "已屏蔽的文件夹 (${blockedFolders.size})",
                                    color = currentAccent.mainColor,
                                    fontSize = DialogTitleFontSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(DialogTitleToContentSpace - DialogItemSpacing))
                        }

                        if (blockedFolders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(itemBg).padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无已屏蔽的文件夹",
                                        color = appColors.textColorSecondary.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            items(blockedFolders.toList()) { folderPath ->
                                val folderName = remember(folderPath) {
                                    val file = java.io.File(folderPath)
                                    file.name.ifEmpty { folderPath }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(itemBg).padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = folderName,
                                            color = appColors.textColorPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = folderPath, color = appColors.textColorSecondary, fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeBlockedFolder(folderPath) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "取消屏蔽",
                                            tint = Color(0xFFE06C75),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 3. Section: 检测到的音乐文件夹
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "检测到的音乐文件夹 (${detectedFolders.size})",
                                    color = currentAccent.mainColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (detectedFolders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(itemBg).padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "未检测到本地音乐文件夹",
                                        color = appColors.textColorSecondary.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            items(detectedFolders) { folderPath ->
                                val folderName = remember(folderPath) {
                                    val file = java.io.File(folderPath)
                                    file.name.ifEmpty { folderPath }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(itemBg).padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = folderName,
                                            color = appColors.textColorPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = folderPath, color = appColors.textColorSecondary, fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.addBlockedFolder(folderPath) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "屏蔽该文件夹",
                                            tint = currentAccent.mainColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showBlockedFoldersDialog = false },
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = appColors.textColorSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // 1. Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCreatePlaylistDialog = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding).border(
                    width = 1.dp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    // Header Area with Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                                .background(currentAccent.mainColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = currentAccent.mainColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "创建新歌单",
                            color = appColors.textColorPrimary,
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                    // Input TextField
                    OutlinedTextField(
                        value = newPlaylistName, onValueChange = { newPlaylistName = it }, placeholder = {
                            Text(
                                "输入歌单名称...",
                                color = appColors.textColorSecondary.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }, singleLine = true, trailingIcon = {
                            if (newPlaylistName.isNotEmpty()) {
                                IconButton(
                                    onClick = { newPlaylistName = "" }, modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清空",
                                        tint = appColors.textColorSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }, colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color(0xFF222225) else Color(0xFFF3F3F5),
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF1B1B1E) else Color(0xFFF9F9FA),
                            focusedTextColor = appColors.textColorPrimary,
                            unfocusedTextColor = appColors.textColorPrimary,
                            focusedBorderColor = currentAccent.mainColor,
                            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(
                                alpha = 0.06f
                            ),
                            cursorColor = currentAccent.mainColor
                        ), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(DialogContentToButtonsSpace))

                    // Action Buttons (Row)
                    val isNameValid = newPlaylistName.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing)
                    ) {
                        // Cancel Button
                        Button(
                            onClick = {
                                showCreatePlaylistDialog = false
                                newPlaylistName = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF28282C) else Color(0xFFEBEBEF),
                                contentColor = appColors.textColorSecondary
                            ),
                            shape = DialogButtonShape,
                            modifier = Modifier.weight(1f).height(DialogButtonHeight),
                            contentPadding = PaddingValues(0.dp)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentAccent.mainColor,
                                contentColor = Color.White,
                                disabledContainerColor = if (isDarkMode) Color(0xFF2E2E33) else Color(0xFFE5E5E9),
                                disabledContentColor = appColors.textColorSecondary.copy(alpha = 0.4f)
                            ),
                            shape = DialogButtonShape,
                            modifier = Modifier.weight(1f).height(DialogButtonHeight),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("创建", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 1.1 Delete Playlist Confirm Dialog
    if (playlistToDelete != null) {
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { playlistToDelete = null }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "删除歌单",
                            color = currentAccent.mainColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "确认要删除歌单「${playlistToDelete!!.name}」吗？\n删除后歌单内的歌曲记录将丢失，该操作无法撤销。",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { playlistToDelete = null }) {
                                Text("取消", color = appColors.textColorSecondary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    val id = playlistToDelete!!.id
                                    viewModel.deletePlaylist(id)
                                    playlistToDelete = null
                                    Toast.makeText(context, "歌单已删除", Toast.LENGTH_SHORT).show()
                                }) {
                                Text(
                                    text = "确认删除",
                                    color = Color(0xFFE06C75),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1.15 Import Playlist Result Dialog
    if (importResult != null) {
        val result = importResult!!
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { importResult = null }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(
                    width = 1.dp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Header Area with Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(
                                if (result.failureCount > 0) Color(0xFFD97706).copy(alpha = 0.12f)
                                else currentAccent.mainColor.copy(alpha = 0.12f)
                            ), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.failureCount > 0) Icons.Default.Warning
                                else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (result.failureCount > 0) Color(0xFFD97706)
                                else currentAccent.mainColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "导入歌单结果",
                            color = appColors.textColorPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "已导入歌单/分组: ${result.playlistsImported} 个",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "成功关联本地文件: ${result.successCount} 首",
                            color = currentAccent.mainColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "未找到本地物理文件: ${result.failureCount} 首",
                            color = if (result.failureCount > 0) Color(0xFFE06C75) else appColors.textColorSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (result.failureCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Failed list section if any failures
                    if (result.failureCount > 0 && result.failedSongs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未找到物理文件的歌曲列表:",
                            color = appColors.textColorSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                                .border(
                                    0.5.dp,
                                    if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                ).padding(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(result.failedSongs) { songText ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                .background(Color(0xFFE06C75))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = songText,
                                            color = appColors.textColorPrimary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Single Confirm Button
                    Button(
                        onClick = { importResult = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentAccent.mainColor, contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("我知道了", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 1.16 Import Playlist Error Dialog
    if (importError != null) {
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { importError = null }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).border(
                    width = 1.dp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE06C75).copy(alpha = 0.12f)), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE06C75),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "导入歌单失败",
                            color = appColors.textColorPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = importError!!, color = appColors.textColorPrimary, fontSize = 13.sp, lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { importError = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE06C75), contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("我知道了", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 1.3 Delete Song From Local Database Confirm Dialog
    if (songToDelete != null) {
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { songToDelete = null }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "删除歌曲",
                            color = Color(0xFFE06C75),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "确定要删除歌曲「${songToDelete!!.title}」吗？",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { songToDelete = null }) {
                                Text("取消", color = appColors.textColorSecondary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    val song = songToDelete!!
                                    viewModel.deleteSong(context, song.id)
                                    songToDelete = null
                                    Toast.makeText(context, "已删除歌曲记录", Toast.LENGTH_SHORT).show()
                                }) {
                                Text(
                                    text = "确认删除",
                                    color = Color(0xFFE06C75),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1.2 Remove Song From Playlist Confirm Dialog
    if (songToRemoveFromPlaylist != null) {
        val dialogBg = appColors.surfaceColor
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { songToRemoveFromPlaylist = null }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "从歌单移除",
                            color = currentAccent.mainColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "确认要将歌曲「${songToRemoveFromPlaylist!!.second.title}」从当前歌单中移出吗？",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { songToRemoveFromPlaylist = null }) {
                                Text("取消", color = appColors.textColorSecondary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    val playlistId = songToRemoveFromPlaylist!!.first
                                    val songId = songToRemoveFromPlaylist!!.second.id
                                    viewModel.removeSongFromPlaylist(playlistId, songId)
                                    songToRemoveFromPlaylist = null
                                    Toast.makeText(context, "已从歌单移除", Toast.LENGTH_SHORT).show()
                                }) {
                                Text(
                                    text = "确认移除",
                                    color = Color(0xFFE06C75),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Add Song To Playlist Dialog
    if (showAddToPlaylistDialog && selectedSongForAddToPlaylist != null) {
        val dialogBg = appColors.surfaceColor
        val itemBg = if (isDarkMode) Color(0x0AFFFFFF) else Color(0x0A000000)

        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                selectedSongForAddToPlaylist = null
            }) {
            androidx.compose.material3.Surface(
                shape = DialogShape,
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "加入到歌单",
                            color = currentAccent.mainColor,
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                        if (playlists.isEmpty()) {
                            Text(
                                text = "暂无自定义歌单。\n请先前往“我的歌单”创建新歌单。",
                                color = appColors.textColorSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                                modifier = Modifier.heightIn(max = 220.dp)
                            ) {
                                items(playlists) { playlist ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                            .background(itemBg).clickable {
                                                viewModel.addSongToPlaylist(
                                                    playlist.id, selectedSongForAddToPlaylist!!.id
                                                )
                                                Toast.makeText(
                                                    context, "已成功加入: ${playlist.name}", Toast.LENGTH_SHORT
                                                ).show()
                                                showAddToPlaylistDialog = false
                                                selectedSongForAddToPlaylist = null
                                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 使用歌单第一歌曲的封面作为图标
                                        PlaylistCover(
                                            firstSongId = playlist.firstSongId,
                                            currentAccent = currentAccent,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1.0f)) {
                                            Text(
                                                text = playlist.name,
                                                color = appColors.textColorPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = "${playlist.songCount} 首歌曲",
                                                color = appColors.textColorSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 右上角极简关闭 X 按钮，与其他弹窗保持一致，无底部冗余“取消”按钮
                    IconButton(
                        onClick = {
                            showAddToPlaylistDialog = false
                            selectedSongForAddToPlaylist = null
                        }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = appColors.textColorSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // 3. Theme Selection Dialog
    if (showThemeDialog) {
        val dialogBg = appColors.surfaceColor

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showThemeDialog = false }) {
            androidx.compose.material3.Surface(
                shape = DialogShape,
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "选择主题颜色",
                            color = currentAccent.mainColor,
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                        // Dark/Light Mode Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing)
                        ) {
                            // Light Mode
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (!isDarkMode) currentAccent.mainColor.copy(alpha = 0.15f) else appColors.cardBackground)
                                    .border(
                                        1.5.dp,
                                        if (!isDarkMode) currentAccent.mainColor else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ).clickable { viewModel.setDarkMode(false) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "日间模式",
                                    color = if (!isDarkMode) currentAccent.mainColor else appColors.textColorSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dark Mode
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkMode) currentAccent.mainColor.copy(alpha = 0.15f) else appColors.cardBackground)
                                    .border(
                                        1.5.dp,
                                        if (isDarkMode) currentAccent.mainColor else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ).clickable { viewModel.setDarkMode(true) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "夜间模式",
                                    color = if (isDarkMode) currentAccent.mainColor else appColors.textColorSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AccentColor.values().forEach { accent ->
                                val isSelected = accent.id == themeName
                                val accentItemBg = if (isSelected) {
                                    accent.mainColor.copy(alpha = 0.12f)
                                } else {
                                    appColors.cardBackground
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(accentItemBg).border(
                                            1.5.dp,
                                            if (isSelected) accent.mainColor else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        ).clickable {
                                            viewModel.setThemeName(accent.id)
                                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Flat color circle
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(accent.mainColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = accent.label,
                                            color = appColors.textColorPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = accent.mainColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showThemeDialog = false }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = appColors.textColorSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
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
            onDismissRequest = { showSleepTimerDialog = false }
        )
    }

    // 检查更新弹窗
    if (showUpdateDialog && latestVersionInfo != null) {
        val info = latestVersionInfo!!
        val dialogBg = appColors.surfaceColor

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showUpdateDialog = false }
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = dialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "发现新版本 ${info.tagName}",
                            color = currentAccent.mainColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "当前版本: v$currentVersionName",
                                color = appColors.textColorSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "•",
                                color = appColors.textColorSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "最新版本: ${info.tagName}",
                                color = currentAccent.mainColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "更新内容：",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(
                                    if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = info.body.ifBlank { "无更新日志说明。" },
                                    color = appColors.textColorPrimary.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showUpdateDialog = false }
                            ) {
                                Text(
                                    text = "以后再说",
                                    color = appColors.textColorSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    showUpdateDialog = false
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(info.htmlUrl)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentAccent.mainColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "立即更新",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showUpdateDialog = false },
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = appColors.textColorSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    val isPlayerVisible = (showFullPlayer || playerOffsetY.value < screenHeight) && currentSong != null

    if (isPlayerVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = playerOffsetY.value
                }
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
                    onSleepTimerClick = { showSleepTimerDialog = true }
                )
            }
        }
    }
}

@Composable
fun QualityBadge(quality: QualityType, isDarkMode: Boolean) {
    val (text, color) = when (quality) {
        QualityType.HiRes -> Pair("Hi-Res", Color(0xFFD97706)) // Gold / Amber
        QualityType.HQ -> Pair("HQ", Color(0xFF0D9488)) // Teal
        else -> return
    }

    val finalBgColor = if (isDarkMode) color.copy(alpha = 0.15f) else color.copy(alpha = 0.08f)
    val finalTextColor = if (isDarkMode) color.copy(alpha = 0.9f) else color

    Box(
        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(finalBgColor).border(
            width = 0.5.dp, color = finalTextColor.copy(alpha = 0.4f), shape = RoundedCornerShape(3.dp)
        ).padding(horizontal = 4.dp, vertical = 0.5.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = finalTextColor, lineHeight = 10.sp
        )
    }
}

@Composable
fun SongItemCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    appColors: AppColors,
    onFavoriteToggle: (() -> Unit)? = null,
    onBlacklistToggle: (() -> Unit)? = null,
    onClick: () -> Unit,
    customActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onCustomAction: (() -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val favoriteColor by animateColorAsState(
        targetValue = if (song.isFavorite) Color(0xFFE06C75) else appColors.textColorSecondary.copy(alpha = 0.5f),
        label = "fav_color"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDarkMode = appColors.surfaceColor == Color(0xFF161619)
    val pressedBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val cardBg = when {
        isCurrent -> {
            val isTitanium = appColors.navBarItemActive == Color(0xFFBAC7D5) || 
                             appColors.navBarItemActive == Color(0xFF4A5568) ||
                             appColors.navBarItemActive == Color(0xFF9AA5B1) ||
                             appColors.navBarItemActive == Color(0xFF8E8E93)
            val alpha = if (isTitanium) 0.20f else 0.12f
            appColors.navBarItemActive.copy(alpha = alpha)
        }
        isPressed -> pressedBgColor
        else -> appColors.cardBackground
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = interactionSource, indication = null, onClick = onClick
        ), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or LiveVisualizer (slightly downscaled to 42.dp for denser presentation)
            SongCover(
                song = song, isCurrent = isCurrent, isPlaying = isPlaying, modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata (Optimized 2-line structure for maximum density and readability)
            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textColorPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                val quality = song.getQualityType()
                Row(
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (quality != QualityType.SQ) {
                        QualityBadge(quality = quality, isDarkMode = isDarkMode)
                    }
                    Text(
                        text = "${song.artist}  •  ${song.album}",
                        fontSize = 11.sp,
                        color = appColors.textColorSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box {
                Box(modifier = Modifier.clip(CircleShape).clickable { showMenu = true }.padding(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "操作菜单",
                        tint = appColors.textColorSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showMenu) {
                    androidx.compose.ui.window.Popup(
                        onDismissRequest = { showMenu = false },
                        alignment = Alignment.TopEnd,
                        offset = androidx.compose.ui.unit.IntOffset(
                            0, with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.roundToPx() }),
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                    ) {
                        Box(
                            modifier = Modifier.width(180.dp).clip(RoundedCornerShape(4.dp))
                                .background(appColors.surfaceColor).border(
                                    0.5.dp,
                                    if (appColors.surfaceColor == Color(0xFF161619)) Color.White.copy(alpha = 0.12f) else Color.Black.copy(
                                        alpha = 0.08f
                                    ),
                                    RoundedCornerShape(4.dp)
                                ).padding(vertical = 4.dp)
                        ) {
                            Column {
                                // 1. Favorite
                                if (onFavoriteToggle != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (song.isFavorite) "取消喜欢" else "设为喜欢",
                                                color = appColors.textColorPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (song.isFavorite) Color(0xFFE06C75) else appColors.textColorSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onFavoriteToggle()
                                            showMenu = false
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                }

                                // 2. Custom action (e.g. Add to Playlist, remove from playlist, restore)
                                if (customActionIcon != null && onCustomAction != null) {
                                    val label = when (customActionIcon) {
                                        Icons.Default.Add -> "加入到歌单"
                                        Icons.Default.Clear -> "从歌单移除"
                                        Icons.Default.Refresh -> "从遗忘的沙漏恢复"
                                        else -> "其他操作"
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label, color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = customActionIcon,
                                                contentDescription = null,
                                                tint = appColors.textColorSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onCustomAction()
                                            showMenu = false
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                }

                                // 3. Forgotten Hourglass
                                if (onBlacklistToggle != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "移至遗忘的沙漏", color = Color(0xFFE5C07B), fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.HourglassEmpty,
                                                contentDescription = null,
                                                tint = Color(0xFFE5C07B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onBlacklistToggle()
                                            showMenu = false
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                }

                                // 3.5. Navigate to Artist
                                if (onNavigateToArtist != null && song.artist.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "歌手：${song.artist}",
                                                color = appColors.textColorPrimary,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = appColors.textColorSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onNavigateToArtist(song.artist)
                                            showMenu = false
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                }

                                // 4. Song Details
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "歌曲详情", color = appColors.textColorPrimary, fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = appColors.textColorSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showDetailsDialog = true
                                        showMenu = false
                                    },
                                    modifier = Modifier.requiredHeight(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                )

                                // 5. Delete Song (from local database only)
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text(text = "删除歌曲", color = Color(0xFFE06C75), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFE06C75),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onDelete()
                                            showMenu = false
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        val sizeMb = song.size / (1024f * 1024f)
        val sizeText = String.format(Locale.getDefault(), "%.2f MB", sizeMb)

        androidx.compose.ui.window.Dialog(onDismissRequest = { showDetailsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding).border(
                    width = 0.5.dp,
                    color = if (appColors.surfaceColor == Color(0xFF161619)) Color.White.copy(alpha = 0.12f) else Color.Black.copy(
                        alpha = 0.08f
                    ),
                    shape = DialogShape
                ),
                shape = DialogShape,
                colors = CardDefaults.cardColors(containerColor = appColors.surfaceColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "歌曲详情",
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = appColors.navBarItemActive
                        )
                        IconButton(
                            onClick = { showDetailsDialog = false }, modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "关闭",
                                tint = appColors.textColorSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                    Column(
                        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DialogItemSpacing)
                    ) {
                        DetailRow(label = "歌名", value = song.title, appColors = appColors)
                        DetailRow(
                            label = "歌手",
                            value = if (song.artist.isBlank()) "未知歌手" else song.artist,
                            appColors = appColors
                        )
                        DetailRow(
                            label = "专辑",
                            value = if (song.album.isBlank()) "未知专辑" else song.album,
                            appColors = appColors
                        )
                        DetailRow(label = "时长", value = formatDuration(song.duration), appColors = appColors)
                        DetailRow(label = "大小", value = sizeText, appColors = appColors)
                        DetailRow(label = "文件路径", value = song.path, appColors = appColors, isPath = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, appColors: AppColors, isPath: Boolean = false) {
    if (isPath) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label, fontSize = 11.sp, color = appColors.textColorSecondary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                color = appColors.textColorPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label, fontSize = 12.sp, color = appColors.textColorSecondary, modifier = Modifier.width(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = appColors.textColorPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1.0f)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val minutes = (durationMs / 1000) / 60
    val seconds = (durationMs / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(), "%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups]
    )
}

data class LyricLine(
    val timeMs: Long, val text: String
)

fun parseLrc(lrcContent: String): List<LyricLine> {
    val lines = lrcContent.split("\n")
    val lyricList = mutableListOf<LyricLine>()
    val timeRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
    for (line in lines) {
        val trimmed = line.trim()
        val matches = timeRegex.findAll(trimmed).toList()
        if (matches.isNotEmpty()) {
            val text = trimmed.replace(timeRegex, "").trim()
            if (text.isNotBlank()) {
                for (match in matches) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val milliStr = match.groupValues[3]
                    var milli = milliStr.toLong()
                    if (milliStr.length == 2) {
                        milli *= 10
                    }
                    val timeMs = min * 60 * 1000 + sec * 1000 + milli
                    lyricList.add(LyricLine(timeMs, text))
                }
            }
        }
    }
    return lyricList.sortedBy { it.timeMs }
}

fun extractLyricsFromMp3(filePath: String): String? {
    val file = java.io.File(filePath)
    if (!file.exists()) return null
    var fis: java.io.FileInputStream? = null
    try {
        fis = java.io.FileInputStream(file)
        val header = ByteArray(10)
        if (fis.read(header) != 10) return null

        if (header[0].toChar() != 'I' || header[1].toChar() != 'D' || header[2].toChar() != '3') {
            return null
        }

        val majorVersion = header[3].toInt()

        val tagSize =
            ((header[6].toInt() and 0x7f) shl 21) or ((header[7].toInt() and 0x7f) shl 14) or ((header[8].toInt() and 0x7f) shl 7) or (header[9].toInt() and 0x7f)

        val maxReadSize = minOf(tagSize, 10 * 1024 * 1024)
        val tagData = ByteArray(maxReadSize)
        var totalRead = 0
        while (totalRead < maxReadSize) {
            val read = fis.read(tagData, totalRead, maxReadSize - totalRead)
            if (read == -1) break
            totalRead += read
        }

        var pos = 0
        while (pos + 10 <= totalRead) {
            if (tagData[pos].toInt() == 0 && tagData[pos + 1].toInt() == 0 && tagData[pos + 2].toInt() == 0 && tagData[pos + 3].toInt() == 0) {
                break
            }

            val frameId = String(tagData, pos, 4, Charsets.US_ASCII)

            val frameSize = if (majorVersion == 3) {
                ((tagData[pos + 4].toInt() and 0xff) shl 24) or ((tagData[pos + 5].toInt() and 0xff) shl 16) or ((tagData[pos + 6].toInt() and 0xff) shl 8) or (tagData[pos + 7].toInt() and 0xff)
            } else {
                ((tagData[pos + 4].toInt() and 0x7f) shl 21) or ((tagData[pos + 5].toInt() and 0x7f) shl 14) or ((tagData[pos + 6].toInt() and 0x7f) shl 7) or (tagData[pos + 7].toInt() and 0x7f)
            }

            if (frameSize <= 0 || pos + 10 + frameSize > totalRead) {
                break
            }

            if (frameId == "USLT") {
                val payload = tagData.sliceArray((pos + 10) until (pos + 10 + frameSize))
                if (payload.isNotEmpty()) {
                    val encodingByte = payload[0].toInt() and 0xff
                    var textOffset = 4
                    if (encodingByte == 0 || encodingByte == 3) {
                        while (textOffset < payload.size && payload[textOffset].toInt() != 0) {
                            textOffset++
                        }
                        textOffset += 1
                    } else {
                        while (textOffset + 1 < payload.size && !(payload[textOffset].toInt() == 0 && payload[textOffset + 1].toInt() == 0)) {
                            textOffset += 2
                        }
                        textOffset += 2
                    }
                    if (textOffset < payload.size) {
                        val charset = when (encodingByte) {
                            0 -> Charsets.ISO_8859_1
                            1 -> Charsets.UTF_16
                            2 -> java.nio.charset.Charset.forName("UTF-16BE")
                            3 -> Charsets.UTF_8
                            else -> Charsets.UTF_8
                        }
                        return String(payload, textOffset, payload.size - textOffset, charset)
                    }
                }
            }

            pos += 10 + frameSize
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {
        }
    }
    return null
}

fun extractLyricsFromFlac(filePath: String): String? {
    val file = java.io.File(filePath)
    if (!file.exists()) return null
    var fis: java.io.FileInputStream? = null
    try {
        fis = java.io.FileInputStream(file)
        val header = ByteArray(4)
        if (fis.read(header) != 4) return null
        if (header[0].toChar() != 'f' || header[1].toChar() != 'L' || header[2].toChar() != 'a' || header[3].toChar() != 'C') {
            return null
        }

        var isLast = false
        while (!isLast) {
            val blockHeader = ByteArray(4)
            if (fis.read(blockHeader) != 4) break

            isLast = (blockHeader[0].toInt() and 0x80) != 0
            val blockType = blockHeader[0].toInt() and 0x7f
            val blockLength =
                ((blockHeader[1].toInt() and 0xff) shl 16) or ((blockHeader[2].toInt() and 0xff) shl 8) or (blockHeader[3].toInt() and 0xff)

            if (blockType == 4) { // VORBIS_COMMENT
                val blockData = ByteArray(blockLength)
                var totalRead = 0
                while (totalRead < blockLength) {
                    val read = fis.read(blockData, totalRead, blockLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }

                var offset = 0
                fun readInt32(): Int {
                    if (offset + 4 > blockLength) return 0
                    val val32 =
                        (blockData[offset].toInt() and 0xff) or ((blockData[offset + 1].toInt() and 0xff) shl 8) or ((blockData[offset + 2].toInt() and 0xff) shl 16) or ((blockData[offset + 3].toInt() and 0xff) shl 24)
                    offset += 4
                    return val32
                }

                val vendorLength = readInt32()
                offset += vendorLength

                val userCommentListLength = readInt32()
                for (i in 0 until userCommentListLength) {
                    val commentLength = readInt32()
                    if (offset + commentLength > blockLength) break
                    val comment = String(blockData, offset, commentLength, Charsets.UTF_8)
                    offset += commentLength

                    val eqIdx = comment.indexOf('=')
                    if (eqIdx != -1) {
                        val key = comment.substring(0, eqIdx).trim().uppercase()
                        if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "LYRIC") {
                            return comment.substring(eqIdx + 1)
                        }
                    }
                }
                break
            } else {
                fis.skip(blockLength.toLong())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {
        }
    }
    return null
}

fun extractLyricsFromM4a(filePath: String): String? {
    val file = java.io.File(filePath)
    if (!file.exists()) return null
    var fis: java.io.FileInputStream? = null
    try {
        fis = java.io.FileInputStream(file)
        val fileLength = file.length()

        fun searchAtom(startOffset: Long, endOffset: Long, targetPath: List<String>, pathIndex: Int): String? {
            var offset = startOffset
            while (offset + 8 <= endOffset) {
                fis?.channel?.position(offset)
                val header = ByteArray(8)
                if (fis?.read(header) != 8) break

                val size =
                    ((header[0].toInt() and 0xff) shl 24) or ((header[1].toInt() and 0xff) shl 16) or ((header[2].toInt() and 0xff) shl 8) or (header[3].toInt() and 0xff)
                val type = String(header, 4, 4, Charsets.US_ASCII)
                val actualSize = if (size == 1) {
                    val size64 = ByteArray(8)
                    fis?.read(size64)
                    var sizeVal = 0L
                    for (i in 0..7) {
                        sizeVal = (sizeVal shl 8) or (size64[i].toLong() and 0xff)
                    }
                    sizeVal
                } else if (size == 0) {
                    endOffset - offset
                } else {
                    size.toLong()
                }

                if (actualSize <= 0) break

                val targetType = targetPath[pathIndex]
                if (type == targetType) {
                    if (pathIndex == targetPath.lastIndex) {
                        val contentStart = offset + (if (size == 1) 16 else 8)
                        val contentEnd = offset + actualSize
                        var childOffset = contentStart
                        while (childOffset + 8 <= contentEnd) {
                            fis?.channel?.position(childOffset)
                            val childHeader = ByteArray(8)
                            fis?.read(childHeader)
                            val childSize =
                                ((childHeader[0].toInt() and 0xff) shl 24) or ((childHeader[1].toInt() and 0xff) shl 16) or ((childHeader[2].toInt() and 0xff) shl 8) or (childHeader[3].toInt() and 0xff)
                            val childType = String(childHeader, 4, 4, Charsets.US_ASCII)
                            if (childType == "data") {
                                val dataContentStart = childOffset + 16
                                val dataLength = childSize - 16
                                if (dataLength > 0) {
                                    val dataBytes = ByteArray(dataLength)
                                    fis?.channel?.position(dataContentStart)
                                    fis?.read(dataBytes)
                                    return String(dataBytes, Charsets.UTF_8)
                                }
                            }
                            childOffset += childSize
                        }
                        return null
                    } else {
                        val containerStart = offset + (if (size == 1) 16 else 8) + (if (type == "meta") 4 else 0)
                        val containerEnd = offset + actualSize
                        val result = searchAtom(containerStart, containerEnd, targetPath, pathIndex + 1)
                        if (result != null) return result
                    }
                }
                offset += actualSize
            }
            return null
        }

        val copyrightLyr =
            String(byteArrayOf(0xA9.toByte(), 0x6C.toByte(), 0x79.toByte(), 0x72.toByte()), Charsets.ISO_8859_1)
        val path = listOf("moov", "udta", "meta", "ilst", copyrightLyr)
        return searchAtom(0L, fileLength, path, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {
        }
    }
    return null
}

fun extractLyricsFromAudio(filePath: String): String? {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp3" -> extractLyricsFromMp3(filePath)
        "flac" -> extractLyricsFromFlac(filePath)
        "m4a", "mp4" -> extractLyricsFromM4a(filePath)
        else -> null
    }
}

fun parsePlainLyrics(lyricsContent: String): List<LyricLine> {
    val lines = lyricsContent.split("\n")
    val list = mutableListOf<LyricLine>()
    var validIndex = 0
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isNotBlank()) {
            list.add(LyricLine(validIndex * 4000L, trimmed))
            validIndex++
        }
    }
    return list
}

fun loadLyricsForSong(songPath: String): List<LyricLine> {
    if (songPath.isBlank()) return emptyList()

    // 内置演示歌曲的歌词 mock，保证开箱即用且体验极佳
    if (songPath.contains("acousticbreeze.mp3")) {
        return listOf(
            LyricLine(0, "Acoustic Breeze - Bensound"),
            LyricLine(3000, "感受清风拂面，享受这一刻的惬意"),
            LyricLine(9000, "这是一首温馨舒适的木吉他合奏乐曲"),
            LyricLine(15000, "轻快、放松且洒满阳光"),
            LyricLine(22000, "吉他的指弹旋律在耳边缓缓流淌"),
            LyricLine(28000, "闭上双眼，拂去一天的尘埃"),
            LyricLine(35000, "生活虽然忙碌，但音乐总能带给您片刻宁静"),
            LyricLine(42000, "感受音符的跳动，像夏日微风穿过树梢"),
            LyricLine(48000, "每一个和弦都是对美好的赞美"),
            LyricLine(55000, "旋律轻柔地升起，伴随微弱的沙锤声"),
            LyricLine(62000, "仿佛置身于蔚蓝的海边，倾听浪花拍岸"),
            LyricLine(70000, "自由自在，心随乐动"),
            LyricLine(78000, "无论身在何方，愿这阵清风伴您左右"),
            LyricLine(85000, "感谢收听 Bensound 免费音乐"),
            LyricLine(92000, "祝您拥有轻松愉快的美好时光...")
        )
    } else if (songPath.contains("sunny.mp3")) {
        return listOf(
            LyricLine(0, "Sunny - Bensound"),
            LyricLine(4000, "充满阳光的一天，从欢快的节奏开始"),
            LyricLine(10000, "尤克里里与轻快的鼓点交织"),
            LyricLine(16000, "像清晨的第一缕晨曦照进房间"),
            LyricLine(22000, "温暖、积极、充满无限可能"),
            LyricLine(29000, "迈出轻快的步伐，向着梦想出发"),
            LyricLine(36000, "把所有烦恼都抛在脑后吧"),
            LyricLine(42000, "今天也是元气满满的一天"),
            LyricLine(49000, "阳光洒在大街小巷，映红了每个人的笑脸"),
            LyricLine(56000, "听！那是快乐的音符在歌唱"),
            LyricLine(63000, "生活因为这些美妙的旋律而熠熠生辉"),
            LyricLine(70000, "享受此刻，晴空万里"),
            LyricLine(77000, "把这份温暖分享给身边的每个人"),
            LyricLine(84000, "感谢收听 Bensound 免费音乐"),
            LyricLine(90000, "愿您的世界每天都洒满阳光")
        )
    } else if (songPath.contains("littleidea.mp3")) {
        return listOf(
            LyricLine(0, "Little Idea - Bensound"),
            LyricLine(3000, "一个小小的创意，正在萌芽"),
            LyricLine(8000, "俏皮的木琴音色与轻柔口哨"),
            LyricLine(14000, "勾勒出充满奇思妙想的画面"),
            LyricLine(20000, "灵感在脑海中轻轻碰撞"),
            LyricLine(26000, "不需要多么宏大的构想"),
            LyricLine(32000, "生活中的小细节，就是最棒的创意来源"),
            LyricLine(38000, "跟着欢快的拍子，一起点头"),
            LyricLine(44000, "让想象力飞往更遥远的地方"),
            LyricLine(50000, "创造属于你自己的独特篇章"),
            LyricLine(56000, "哪怕只是一个小点子，也能改变世界"),
            LyricLine(62000, "音符在指尖跳跃，带您重回童真"),
            LyricLine(68000, "快乐其实很简单，只需一点点奇思妙想"),
            LyricLine(74000, "感谢收听 Bensound 免费音乐"),
            LyricLine(80000, "释放你的创意，下一秒会更精彩！")
        )
    }

    // 1. 优先加载本地同名同目录的外部 .lrc 文件
    try {
        val lrcPath = songPath.substringBeforeLast(".") + ".lrc"
        val file = java.io.File(lrcPath)
        if (file.exists()) {
            val parsed = parseLrc(file.readText(Charsets.UTF_8))
            if (parsed.isNotEmpty()) return parsed
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // 2. 次优先从音频元数据（内嵌歌词）中解析
    val embeddedLyrics = extractLyricsFromAudio(songPath)
    if (!embeddedLyrics.isNullOrBlank()) {
        val parsed = parseLrc(embeddedLyrics)
        if (parsed.isNotEmpty()) {
            return parsed
        } else {
            // 兜底：如果是没有时间戳的纯文本歌词，按行分割并虚拟分配时间戳间隔（每行4秒）
            return parsePlainLyrics(embeddedLyrics)
        }
    }

    return emptyList()
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricView(
    song: Song, playbackProgress: Long, appColors: AppColors, currentAccent: AccentColor, onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    var lyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var hasLoaded by remember(song.id) { mutableStateOf(false) }
    var isClickSeeking by remember(song.id) { mutableStateOf(false) }
    var clickedActiveIndex by remember(song.id) { mutableStateOf<Int?>(null) }
    var seekJob by remember(song.id) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(song.id, song.path) {
        withContext(Dispatchers.IO) {
            lyrics = loadLyricsForSong(song.path)
            hasLoaded = true
        }
    }

    if (!hasLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = currentAccent.mainColor)
        }
    } else if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = appColors.textColorSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "暂无歌词", color = appColors.textColorSecondary, fontSize = 15.sp
                )
            }
        }
    } else {
        val activeIndex = remember(lyrics, playbackProgress, clickedActiveIndex) {
            if (clickedActiveIndex != null) {
                clickedActiveIndex!!
            } else {
                val idx = lyrics.indexOfLast { it.timeMs <= playbackProgress }
                if (idx == -1) 0 else idx
            }
        }

        val lyricListState = rememberLazyListState()
        val isUserScrolling by lyricListState.interactionSource.collectIsDraggedAsState()
        var isAutoFollowPaused by remember(song.id) { mutableStateOf(false) }

        LaunchedEffect(isUserScrolling) {
            if (isUserScrolling) {
                isAutoFollowPaused = true
            }
        }

        // 同时监听 activeIndex 和 isClickSeeking：
        // 当 isClickSeeking 从 true → false 时也会重触发，
        // 确保 seek 锁释放后即使 activeIndex 值未变也能恢复自动滚动跟随
        LaunchedEffect(activeIndex, isClickSeeking) {
            if (lyrics.isNotEmpty() && !lyricListState.isScrollInProgress && !isClickSeeking && !isAutoFollowPaused) {
                lyricListState.animateScrollToItem(activeIndex)
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 24.dp)
        ) {
            val halfHeight = maxHeight / 2

            LazyColumn(
                state = lyricListState,
                contentPadding = PaddingValues(top = halfHeight - 24.dp, bottom = halfHeight - 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) {
                            currentAccent.mainColor
                        } else {
                            appColors.textColorPrimary.copy(alpha = 0.5f)
                        },
                        animationSpec = tween(durationMillis = 350),
                        label = "lyric_color"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.15f else 1.0f,
                        animationSpec = tween(durationMillis = 350),
                        label = "lyric_scale"
                    )
                    val scaleAlphaValue by animateFloatAsState(
                        targetValue = if (isActive) 1.0f else 0.4f,
                        animationSpec = tween(durationMillis = 350),
                        label = "lyric_alpha"
                    )

                    Text(
                        text = line.text,
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.8f).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = scaleAlphaValue
                        }.clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null
                        ) {
                            clickedActiveIndex = index
                            isClickSeeking = true
                            isAutoFollowPaused = false
                            onSeek(line.timeMs)
                            seekJob?.cancel()
                            seekJob = coroutineScope.launch {
                                try {
                                    lyricListState.animateScrollToItem(index)
                                    kotlinx.coroutines.delay(1000)
                                    // 必须先清 isClickSeeking，再清 clickedActiveIndex：
                                    // clickedActiveIndex = null 会触发 LaunchedEffect(activeIndex, isClickSeeking)，
                                    // 此时 isClickSeeking 若仍为 true 则滚动条件不满足，自动跟随无法恢复
                                    isClickSeeking = false
                                    clickedActiveIndex = null
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    // 被取消说明有新的点击，直接由新的协程去更新
                                }
                            }
                        }.padding(vertical = 4.dp)
                    )
                }
            }

            // 右下角重新跟随按钮（使用属性动画平滑渐显和缩放，防止 AnimatedVisibility 引起的边界收缩裁剪）
            val buttonAlpha by animateFloatAsState(
                targetValue = if (isAutoFollowPaused) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "follow_btn_alpha"
            )
            val buttonScale by animateFloatAsState(
                targetValue = if (isAutoFollowPaused) 1f else 0.8f,
                animationSpec = tween(durationMillis = 300),
                label = "follow_btn_scale"
            )

            if (buttonAlpha > 0.01f) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 4.dp)
                        .graphicsLayer {
                            alpha = buttonAlpha
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .size(42.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(currentAccent.mainColor.copy(alpha = 0.9f))
                        .clickable {
                            isAutoFollowPaused = false
                            coroutineScope.launch {
                                if (lyrics.isNotEmpty()) {
                                    lyricListState.animateScrollToItem(activeIndex)
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "重新跟随",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackProgress: Long,
    viewModel: SongViewModel,
    appColors: AppColors,
    isDarkMode: Boolean,
    currentAccent: AccentColor,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    dragModifier: Modifier,
    onSleepTimerClick: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = 0, pageCount = { 2 })

    // 动态炫光背景的无限循环动画（通过 graphicsLayer 更新，不触发 recomposition，极低功耗）
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 2.4f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowTranslationX by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationX"
    )
    val glowTranslationY by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationY"
    )
    var bgBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(song.getCachedCover()) }
    var isCoverLoaded by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }
    // 封面透明度 Animatable：控制唱片中心封面与背景模糊图的淡出/淡入效果
    val coverAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    // 标记是否是首次加载，避免进入播放界面时触发淡出动画
    var isFirstLoad by remember { mutableStateOf(true) }

    var showPlaybackQueueDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val playbackQueue by viewModel.playbackQueue.collectAsState()

    LaunchedEffect(song.id) {
        if (isFirstLoad) {
            // 首次加载：直接加载封面，不做淡出动画
            isFirstLoad = false
            isCoverLoaded = false
            rotationAngle = 0f
            withContext(Dispatchers.IO) {
                val loaded = song.loadCover(context, 400)
                withContext(Dispatchers.Main) {
                    bgBitmap = loaded
                    withFrameMillis {}
                    withFrameMillis {}
                    isCoverLoaded = true
                }
            }
        } else {
            // 切歌：先淡出当前封面（旋转继续）-> 停转重置 -> 加载新封面 -> 淡入
            // 1. 直接开始淡出（200ms），旋转随封面一起淡出，不提前停顿
            coverAlpha.animateTo(
                targetValue = 0f, animationSpec = tween(200)
            )
            // 2. 淡出完成后停止旋转并重置角度
            isCoverLoaded = false
            rotationAngle = 0f
            // 3. 加载下一首封面
            val loaded = withContext(Dispatchers.IO) { song.loadCover(context, 400) }
            bgBitmap = loaded
            withFrameMillis {}
            withFrameMillis {}
            isCoverLoaded = true
            // 4. 淡入新封面（300ms）
            coverAlpha.animateTo(
                targetValue = 1f, animationSpec = tween(300)
            )
        }
    }

    // 平滑卡片缩放呼吸效果与发光深度
    val cardScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 0.95f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        ), label = "cardScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.5f else 0.15f, animationSpec = tween(800), label = "glowAlpha"
    )

    val playMode by viewModel.playMode.collectAsState()
    val favoriteColor by animateColorAsState(
        targetValue = if (song.isFavorite) Color(0xFFE06C75) else appColors.textColorSecondary.copy(alpha = 0.6f),
        label = "fav_btn"
    )

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(appColors.mainBackground).then(dragModifier)
    ) {
        // 1. Ambient blur cover image（放大并提高不透明度与模糊度，制作高保真炫光背景）
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        rotationZ = glowRotation
                        translationX = glowTranslationX
                        translationY = glowTranslationY
                        alpha = 0.55f * coverAlpha.value
                    },
                contentScale = ContentScale.Crop
            )
        }

        // 精心调配的多重渐变遮罩，使封面色彩流淌的同时保证文字的可读性
        val overlayBrush = if (isDarkMode) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xCC0C0C0E), // 顶部暗色，适合状态栏和操作按钮
                    Color(0x550C0C0E), // 中间极透明，展现封面中心色彩
                    Color(0xEE0C0C0E)  // 底部暗色，适合播放器控制盘和歌词底色
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xCCF5F5F7), // 顶部
                    Color(0x55F5F5F7), // 中间
                    Color(0xEEF5F5F7)  // 底部
                )
            )
        }

        val radialOverlayBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                if (isDarkMode) Color(0xAA0C0C0E) else Color(0xAAF5F5F7)
            )
        )

        Box(
            modifier = Modifier.fillMaxSize().background(overlayBrush)
        )
        Box(
            modifier = Modifier.fillMaxSize().background(radialOverlayBrush)
        )

        // Main content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar
            val topBtnBg = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000)
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(topBtnBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "收起",
                        tint = appColors.textColorPrimary
                    )
                }

                Text(
                    text = if (isPlaying) "正在播放" else "已暂停",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textColorPrimary.copy(alpha = 0.9f),
                    letterSpacing = 0.5.sp
                )

                // Right menu dropdown
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(topBtnBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = appColors.textColorPrimary
                        )
                    }
                    if (showMoreMenu) {
                        androidx.compose.ui.window.Popup(
                            onDismissRequest = { showMoreMenu = false },
                            alignment = Alignment.TopEnd,
                            offset = androidx.compose.ui.unit.IntOffset(
                                0, with(androidx.compose.ui.platform.LocalDensity.current) { 44.dp.roundToPx() }),
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                        ) {
                            Box(
                                modifier = Modifier.width(140.dp).clip(RoundedCornerShape(4.dp))
                                    .background(appColors.surfaceColor).border(
                                        0.5.dp,
                                        if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        RoundedCornerShape(4.dp)
                                    ).padding(vertical = 4.dp)
                            ) {
                                Column {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "加入到歌单", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onAddToPlaylistClick(song)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistAdd,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "歌曲详情", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showDetailsDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "定时关闭", color = appColors.textColorPrimary, fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onSleepTimerClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = appColors.textColorPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.requiredHeight(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    )
                                    if (song.artist.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "歌手：${song.artist}",
                                                    color = appColors.textColorPrimary,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                onNavigateToArtist(song.artist)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = appColors.textColorPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.requiredHeight(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. HorizontalPager for Vinyl Disc (Page 0) and Lyrics (Page 1)
            HorizontalPager(
                state = pagerState, modifier = Modifier.weight(1.2f).fillMaxWidth()
            ) { page ->
                if (page == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        val shouldRotate = isPlaying && isCoverLoaded
                        LaunchedEffect(shouldRotate) {
                            if (shouldRotate) {
                                val startFrameTime = android.os.SystemClock.uptimeMillis()
                                val startAngle = rotationAngle
                                while (true) {
                                    withFrameMillis { frameTime ->
                                        val elapsed = android.os.SystemClock.uptimeMillis() - startFrameTime
                                        rotationAngle = (startAngle + elapsed * 0.024f) % 360f
                                    }
                                }
                            }
                        }

                        // Vinyl outer disc
                        Box(
                            modifier = Modifier.size(300.dp).graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                                rotationZ = rotationAngle
                            }.clip(CircleShape).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isPlaying) {
                                    viewModel.pauseSong()
                                } else {
                                    viewModel.resumeSong()
                                }
                            }.background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2C2C2E), Color(0xFF151517), Color(0xFF0C0C0E)
                                    )
                                )
                            ).border(4.dp, Color(0xFF232325), CircleShape)
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // 同心圆声轨线条
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val ringCount = 8
                                val startRadius = 102.dp.toPx()
                                val endRadius = 142.dp.toPx()
                                for (i in 0 until ringCount) {
                                    val r = startRadius + (endRadius - startRadius) * (i.toFloat() / (ringCount - 1))
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.03f),
                                        radius = r,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                                    )
                                }
                            }

                            // Center album cover image（应用 coverAlpha 淡出/淡入效果）
                            Box(
                                modifier = Modifier.size(200.dp).clip(CircleShape).background(Color(0xFF1C1C1E))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .graphicsLayer { alpha = coverAlpha.value }) {
                                if (bgBitmap != null) {
                                    Image(
                                        bitmap = bgBitmap!!.asImageBitmap(),
                                        contentDescription = "Cover Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    currentAccent.mainColor.copy(alpha = 0.8f),
                                                    currentAccent.mainColor.copy(alpha = 0.4f)
                                                )
                                            )
                                        ), contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LyricView(
                        song = song,
                        playbackProgress = playbackProgress,
                        appColors = appColors,
                        currentAccent = currentAccent,
                        onSeek = { position -> viewModel.seekTo(position) })
                }
            }

            // 3. Centered Song Metadata & Symmetric Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Action: Blacklist (Forgotten Hourglass)
                IconButton(
                    onClick = {
                        viewModel.addToBlacklist(context, song)
                    },
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "遗忘的沙漏",
                        tint = Color(0xFFE5C07B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Centered song details
                Column(
                    modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textColorPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val quality = song.getQualityType()
                    Row(
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                    ) {
                        if (quality != QualityType.SQ) {
                            QualityBadge(quality = quality, isDarkMode = isDarkMode)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = song.artist,
                            fontSize = 14.sp,
                            color = appColors.textColorSecondary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Action: Favorite
                IconButton(
                    onClick = { viewModel.toggleFavorite(song, skipListUpdate = true) },
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "喜欢",
                        tint = favoriteColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Custom Refined Progress Bar (Canvas-based)
            var sliderPosition by remember { mutableStateOf<Float?>(null) }
            val currentPos = sliderPosition?.toLong() ?: playbackProgress
            val progressPercent = if (song.duration > 0) currentPos.toFloat() / song.duration.toFloat() else 0f
            var progressBarWidth by remember { mutableStateOf(1f) }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                        .onGloballyPositioned { progressBarWidth = it.size.width.toFloat() }
                        .pointerInput(song.duration) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val fraction = (offset.x / progressBarWidth).coerceIn(0f, 1f)
                                    sliderPosition = fraction * song.duration
                                    tryAwaitRelease()
                                    sliderPosition?.let { viewModel.seekTo(it.toLong()) }
                                    sliderPosition = null
                                })
                        }.pointerInput(song.duration) {
                            detectDragGestures(onDragStart = { offset ->
                                val fraction = (offset.x / progressBarWidth).coerceIn(0f, 1f)
                                sliderPosition = fraction * song.duration
                            }, onDrag = { change, _ ->
                                val fraction = (change.position.x / progressBarWidth).coerceIn(0f, 1f)
                                sliderPosition = fraction * song.duration
                            }, onDragEnd = {
                                sliderPosition?.let { viewModel.seekTo(it.toLong()) }
                                sliderPosition = null
                            }, onDragCancel = {
                                sliderPosition = null
                            })
                        }, contentAlignment = Alignment.CenterStart
                ) {
                    val trackColor =
                        if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                    val activeColor = currentAccent.mainColor

                    Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                        val width = size.width
                        val height = size.height
                        val cap = StrokeCap.Round

                        // Background track
                        drawLine(
                            color = trackColor,
                            start = Offset(0f, height / 2),
                            end = Offset(width, height / 2),
                            strokeWidth = height,
                            cap = cap
                        )

                        // Active progress track
                        drawLine(
                            color = activeColor,
                            start = Offset(0f, height / 2),
                            end = Offset(width * progressPercent, height / 2),
                            strokeWidth = height,
                            cap = cap
                        )
                    }

                    // Elegant micro floating Thumb (Fixed Order to prevent double-circle bug, no shadow)
                    val thumbOffset = progressBarWidth * progressPercent
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = (thumbOffset - 5.dp.toPx()).coerceAtLeast(0f)
                        }.size(10.dp).background(Color.White, CircleShape)
                            .border(0.5.dp, currentAccent.mainColor.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPos),
                        color = appColors.textColorSecondary.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDuration(song.duration),
                        color = appColors.textColorSecondary.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Playback Controller Buttons (Symmetric & Lightweight)
            val playBtnIconColor = Color.White

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Play Mode button (Flat Style)
                IconButton(
                    onClick = {
                        viewModel.togglePlayMode()
                        val modeStr = when (playMode) {
                            PlayMode.ListLoop -> "单曲循环"
                            PlayMode.SingleLoop -> "随机播放"
                            PlayMode.Shuffle -> "列表循环"
                        }
                        Toast.makeText(context, "播放模式已切至: $modeStr", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(44.dp)
                ) {
                    val modeIcon = when (playMode) {
                        PlayMode.ListLoop -> Icons.Default.Repeat
                        PlayMode.SingleLoop -> Icons.Default.RepeatOne
                        PlayMode.Shuffle -> Icons.Default.Shuffle
                    }
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = "播放模式",
                        tint = appColors.textColorPrimary.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Control Group (Previous, Play/Pause with custom glow, Next)
                Row(
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Previous
                    IconButton(
                        onClick = { viewModel.playPreviousSong(context) }, modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            tint = appColors.textColorPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Glow Play / Pause
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentAccent.mainColor.copy(alpha = 0.4f), Color.Transparent
                                    )
                                )
                            ).blur(8.dp)
                        )
                        Box(
                            modifier = Modifier.size(62.dp).clip(CircleShape).background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        currentAccent.mainColor, currentAccent.mainColor.copy(alpha = 0.85f)
                                    )
                                )
                            ).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable {
                                if (isPlaying) {
                                    viewModel.pauseSong()
                                } else {
                                    viewModel.resumeSong()
                                }
                            }, contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "播放暂停",
                                tint = playBtnIconColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = { viewModel.playNextSong(context) }, modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = appColors.textColorPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // 3. Playback Queue button (Flat Style)
                IconButton(
                    onClick = { showPlaybackQueueDialog = true }, modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "播放列表",
                        tint = appColors.textColorPrimary.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bottom padding for virtual navigation bar avoidance
            Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    // 7. Current Playback Queue Dialog
    if (showPlaybackQueueDialog) {
        val qDialogBg = appColors.surfaceColor
        val qItemHighlightBg = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0D000000)
        val queueListState = rememberLazyListState()

        LaunchedEffect(Unit) {
            val currentIndex = playbackQueue.indexOfFirst { it.id == song.id }
            if (currentIndex != -1) {
                queueListState.scrollToItem(currentIndex)
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlaybackQueueDialog = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = qDialogBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "当前播放列表",
                                color = currentAccent.mainColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${playbackQueue.size} 首",
                                color = appColors.textColorSecondary.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (playbackQueue.isEmpty()) {
                            Text(
                                text = "播放列表为空", color = appColors.textColorSecondary, fontSize = 13.sp
                            )
                        } else {
                            LazyColumn(
                                state = queueListState,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                            ) {
                                items(playbackQueue) { qSong ->
                                    val isCurrent = qSong.id == song.id
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.playSong(context, qSong)
                                        }.clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) qItemHighlightBg else Color.Transparent)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.0f)) {
                                            Text(
                                                text = qSong.title,
                                                color = if (isCurrent) currentAccent.mainColor else appColors.textColorPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = qSong.artist,
                                                color = appColors.textColorSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.removeFromPlaybackQueue(qSong.id, context)
                                                Toast.makeText(context, "已从播放列表移出", Toast.LENGTH_SHORT).show()
                                            }, modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "移出播放列表",
                                                tint = appColors.textColorSecondary,
                                                modifier = Modifier.size(16.dp)
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
    }

    if (showDetailsDialog) {
        val sizeMb = song.size / (1024f * 1024f)
        val sizeText = String.format(Locale.getDefault(), "%.2f MB", sizeMb)

        androidx.compose.ui.window.Dialog(onDismissRequest = { showDetailsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DialogHorizontalPadding).border(
                    width = 0.5.dp,
                    color = if (appColors.surfaceColor == Color(0xFF161619)) Color.White.copy(alpha = 0.12f) else Color.Black.copy(
                        alpha = 0.08f
                    ),
                    shape = DialogShape
                ),
                shape = DialogShape,
                colors = CardDefaults.cardColors(containerColor = appColors.surfaceColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "歌曲详情",
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = appColors.navBarItemActive
                        )
                        IconButton(
                            onClick = { showDetailsDialog = false }, modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "关闭",
                                tint = appColors.textColorSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                    Column(
                        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DialogItemSpacing)
                    ) {
                        DetailRow(label = "歌名", value = song.title, appColors = appColors)
                        DetailRow(
                            label = "歌手",
                            value = if (song.artist.isBlank()) "未知歌手" else song.artist,
                            appColors = appColors
                        )
                        DetailRow(
                            label = "专辑",
                            value = if (song.album.isBlank()) "未知专辑" else song.album,
                            appColors = appColors
                        )
                        DetailRow(label = "时长", value = formatDuration(song.duration), appColors = appColors)
                        DetailRow(label = "大小", value = sizeText, appColors = appColors)
                        DetailRow(label = "文件路径", value = song.path, appColors = appColors, isPath = true)
                    }
                }
            }
        }
    }
}

// 冗余定义已搬迁至 PinyinHelper.kt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterHeader(letter: Char, appColors: AppColors) {
    Box(
        modifier = Modifier.fillMaxWidth().background(appColors.mainBackground)
            .padding(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(
            text = letter.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textColorSecondary.copy(alpha = 0.8f)
        )
    }
}

fun getTargetIndex(letter: Char, indexMap: Map<Char, Int>): Int {
    if (indexMap.containsKey(letter)) return indexMap[letter]!!
    if (letter == '#') return -1
    val alphabet = ('A'..'Z').toList()
    val letterIndex = alphabet.indexOf(letter)
    if (letterIndex != -1) {
        for (i in letterIndex + 1 until alphabet.size) {
            val nextLetter = alphabet[i]
            if (indexMap.containsKey(nextLetter)) {
                return indexMap[nextLetter]!!
            }
        }
        if (indexMap.containsKey('#')) {
            return indexMap['#']!!
        }
    }
    return -1
}

@Composable
fun SleepTimerDialog(
    viewModel: cn.xuexc.ai_player.ui.SongViewModel,
    appColors: AppColors,
    isDarkMode: Boolean,
    currentAccent: AccentColor,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dialogBg = appColors.surfaceColor
    val remainingMs by viewModel.sleepTimerRemaining.collectAsState()
    val playComplete by viewModel.sleepTimerPlayComplete.collectAsState()

    var localPlayComplete by remember { mutableStateOf(playComplete) }
    var customMinutes by remember { mutableStateOf(30f) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(16.dp),
            color = dialogBg,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "定时关闭",
                        color = currentAccent.mainColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (remainingMs > 0L) currentAccent.mainColor.copy(alpha = 0.1f)
                                else if (isDarkMode) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.animation.Crossfade(
                            targetState = remainingMs > 0L,
                            label = "sleep_timer_state_crossfade"
                        ) { isRunning ->
                            if (isRunning) {
                                val minutes = remainingMs / 1000 / 60
                                val seconds = (remainingMs / 1000) % 60
                                Row(
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "倒计时中：${minutes}分${seconds}秒",
                                        color = appColors.textColorPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "取消定时",
                                        color = currentAccent.mainColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = androidx.compose.ui.Modifier.clickable {
                                            viewModel.cancelSleepTimer()
                                            android.widget.Toast.makeText(
                                                context,
                                                "定时关闭已取消",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            } else {
                                Row(
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "定时关闭未开启",
                                        color = appColors.textColorSecondary,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = appColors.textColorSecondary.copy(alpha = 0.5f),
                                        modifier = androidx.compose.ui.Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(0.5.dp)
                            .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                    )

                    Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自定义定时时间",
                                color = appColors.textColorSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${customMinutes.toInt()} 分钟",
                                color = currentAccent.mainColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        androidx.compose.material3.Slider(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            valueRange = 1f..120f,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = currentAccent.mainColor,
                                activeTrackColor = currentAccent.mainColor,
                                inactiveTrackColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(
                                    alpha = 0.05f
                                )
                            )
                        )
                        Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { localPlayComplete = !localPlayComplete }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "播放完整首再关闭",
                                color = appColors.textColorPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            androidx.compose.material3.Switch(
                                checked = localPlayComplete,
                                onCheckedChange = { localPlayComplete = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = currentAccent.mainColor,
                                    uncheckedThumbColor = appColors.textColorSecondary,
                                    uncheckedTrackColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(
                                        alpha = 0.05f
                                    )
                                ),
                                modifier = androidx.compose.ui.Modifier.graphicsLayer {
                                    scaleX = 0.85f
                                    scaleY = 0.85f
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                                }
                            )
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.startSleepTimer(customMinutes.toLong() * 60 * 1000L, localPlayComplete)
                                onDismissRequest()
                                android.widget.Toast.makeText(
                                    context,
                                    "已设置 ${customMinutes.toInt()} 分钟后关闭",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = currentAccent.mainColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("开启自定义定时", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = androidx.compose.ui.Modifier.align(Alignment.TopEnd).size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = appColors.textColorSecondary,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}