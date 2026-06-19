package cn.xuexc.ai_player.ui.components

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.data.*
import cn.xuexc.ai_player.playback.PlayMode
import cn.xuexc.ai_player.ui.SongViewModel
import java.io.File
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onSleepTimerClick: () -> Unit,
    isFullyHidden: () -> Boolean = { false },
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val currentProgressState = rememberUpdatedState(playbackProgress)
    val progressProvider = remember { { currentProgressState.value } }

    // 1. 极光背景主色状态与平滑颜色插值
    val initialColors =
        remember(song.id, song.lastModified) {
            val cached = song.getCachedBlurredCover()
            if (cached != null && cached.width > 0 && cached.height > 0) {
                try {
                    val w = cached.width
                    val h = cached.height
                    Pair(
                        Color(cached.getPixel(w / 4, h / 4)),
                        Color(cached.getPixel(w * 3 / 4, h * 3 / 4)),
                    )
                } catch (e: Exception) {
                    Pair(
                        currentAccent.gradientColors.firstOrNull() ?: currentAccent.mainColor,
                        currentAccent.gradientColors.lastOrNull() ?: currentAccent.mainColor,
                    )
                }
            } else {
                Pair(
                    currentAccent.gradientColors.firstOrNull() ?: currentAccent.mainColor,
                    currentAccent.gradientColors.lastOrNull() ?: currentAccent.mainColor,
                )
            }
        }

    var glowColor1 by remember(song.id, song.lastModified) { mutableStateOf(initialColors.first) }
    var glowColor2 by remember(song.id, song.lastModified) { mutableStateOf(initialColors.second) }

    val animatedGlowColor1 by
        animateColorAsState(
            targetValue = glowColor1,
            animationSpec = tween(1200),
            label = "animatedColor1",
        )
    val animatedGlowColor2 by
        animateColorAsState(
            targetValue = glowColor2,
            animationSpec = tween(1200),
            label = "animatedColor2",
        )

    fun updateGlowColors(bitmap: android.graphics.Bitmap?) {
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
            try {
                val w = bitmap.width
                val h = bitmap.height
                glowColor1 = Color(bitmap.getPixel(w / 4, h / 4))
                glowColor2 = Color(bitmap.getPixel(w * 3 / 4, h * 3 / 4))
            } catch (e: Exception) {
                glowColor1 = currentAccent.gradientColors.firstOrNull() ?: currentAccent.mainColor
                glowColor2 = currentAccent.gradientColors.lastOrNull() ?: currentAccent.mainColor
            }
        } else {
            glowColor1 = currentAccent.gradientColors.firstOrNull() ?: currentAccent.mainColor
            glowColor2 = currentAccent.gradientColors.lastOrNull() ?: currentAccent.mainColor
        }
    }

    // 2. 动态极光斑的低频呼吸与漂移无限循环动画
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glow1X by
        infiniteTransition.animateFloat(
            initialValue = -0.45f,
            targetValue = 0.45f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(25000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow1X",
        )
    val glow1Y by
        infiniteTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 0.35f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow1Y",
        )
    val glow1RadiusScale by
        infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.05f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow1RadiusScale",
        )
    val glow2X by
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = -0.45f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(22000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow2X",
        )
    val glow2Y by
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = -0.35f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow2Y",
        )
    val glow2RadiusScale by
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow2RadiusScale",
        )
    var bgBitmap by
        remember(song.id, song.lastModified) {
            mutableStateOf<android.graphics.Bitmap?>(song.getCachedCover())
        }
    var bgBitmapBlur by
        remember(song.id, song.lastModified) {
            mutableStateOf<android.graphics.Bitmap?>(song.getCachedBlurredCover())
        }
    var isCoverLoaded by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }
    // 封面透明度 Animatable：控制唱片中心封面与背景模糊图的淡出/淡入效果
    val coverAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    // 标记是否是首次加载，避免进入播放界面时触发淡出动画
    var isFirstLoad by remember { mutableStateOf(true) }
    var lastSongId by remember { mutableStateOf<Long?>(null) }

    var showPlaybackQueueDialog by remember { mutableStateOf(false) }
    val playbackQueue by viewModel.playbackQueue.collectAsState()

    LaunchedEffect(song.id, song.lastModified) {
        if (isFirstLoad) {
            // 首次加载：直接加载封面，不做淡出动画
            isFirstLoad = false
            lastSongId = song.id
            isCoverLoaded = false
            rotationAngle = 0f
            withContext(Dispatchers.IO) {
                val loaded = song.loadCover(context, 400)
                val blurred = loaded?.let { song.loadBlurredCover(context, it) }
                withContext(Dispatchers.Main) {
                    bgBitmap = loaded
                    bgBitmapBlur = blurred
                    updateGlowColors(blurred)
                    withFrameMillis {}
                    withFrameMillis {}
                    isCoverLoaded = true
                }
            }
        } else if (lastSongId != song.id) {
            // 切歌：先淡出当前封面（旋转继续）-> 停转重置 -> 加载新封面 -> 淡入
            // 1. 直接开始淡出（200ms），旋转随封面一起淡出，不提前停顿
            lastSongId = song.id

            // 启动并行的淡出动画
            val fadeOutJob = launch {
                coverAlpha.animateTo(targetValue = 0f, animationSpec = tween(200))
            }

            // 与淡出动画并行执行封面加载
            val loaded = withContext(Dispatchers.IO) { song.loadCover(context, 400) }
            val blurred = loaded?.let { song.loadBlurredCover(context, it) }

            // 等待淡出动画播放完毕
            fadeOutJob.join()

            // 2. 淡出完成后停止旋转并重置角度
            isCoverLoaded = false
            rotationAngle = 0f

            // 3. 替换新封面
            bgBitmap = loaded
            bgBitmapBlur = blurred
            updateGlowColors(blurred)
            withFrameMillis {}
            withFrameMillis {}
            isCoverLoaded = true

            // 4. 淡入新封面（300ms）
            coverAlpha.animateTo(targetValue = 1f, animationSpec = tween(300))
        } else {
            // 同一首歌但 lastModified 改变（标签/封面热更新）
            // 1. 直接开始淡出封面（200ms），但不重置旋转角度 and isCoverLoaded
            coverAlpha.animateTo(targetValue = 0f, animationSpec = tween(200))
            // 2. 加载新封面
            val loaded = withContext(Dispatchers.IO) { song.loadCover(context, 400) }
            val blurred = loaded?.let { song.loadBlurredCover(context, it) }
            bgBitmap = loaded
            bgBitmapBlur = blurred
            updateGlowColors(blurred)
            withFrameMillis {}
            withFrameMillis {}
            // 3. 淡入新封面（300ms）
            coverAlpha.animateTo(targetValue = 1f, animationSpec = tween(300))
        }
    }

    // 平滑卡片缩放呼吸效果与发光深度
    val cardScale by
        animateFloatAsState(
            targetValue = if (isPlaying) 1.05f else 0.95f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "cardScale",
        )

    val glowAlpha by
        animateFloatAsState(
            targetValue = if (isPlaying) 0.5f else 0.15f,
            animationSpec = tween(800),
            label = "glowAlpha",
        )

    val playMode by viewModel.playMode.collectAsState()
    val favoriteColor by
        animateColorAsState(
            targetValue =
                if (song.isFavorite) Color(0xFFE06C75)
                else appColors.textColorSecondary.copy(alpha = 0.6f),
            label = "fav_btn",
        )

    Box(
        modifier =
            Modifier.fillMaxSize()
                .clipToBounds()
                .background(appColors.mainBackground)
                .drawWithContent {
                    if (!isFullyHidden()) {
                        val w = size.width
                        val h = size.height
                        val baseRadius = maxOf(w, h) * 0.45f // 缩减基准半径以使边缘掠过时有更强的边界光感流动

                        drawIntoCanvas { canvas ->
                            // 绘制极光 1 (偏左上漂移，限制在左上象限活动，避免越界和被遮盖)
                            val center1 =
                                Offset(
                                    x = w * 0.25f + w * (glow1X * 0.35f),
                                    y = h * 0.25f + h * (glow1Y * 0.35f),
                                )
                            val radius1 = baseRadius * glow1RadiusScale

                            val paint1 =
                                Paint().apply {
                                    isAntiAlias = true
                                    asFrameworkPaint().isDither = true // 开启硬件级混色抖动
                                }

                            val alpha1 = 0.38f * coverAlpha.value
                            val colors1 =
                                listOf(
                                    animatedGlowColor1.copy(alpha = alpha1),
                                    animatedGlowColor1.copy(alpha = alpha1 * 0.8f),
                                    animatedGlowColor1.copy(alpha = alpha1 * 0.55f),
                                    animatedGlowColor1.copy(alpha = alpha1 * 0.32f),
                                    animatedGlowColor1.copy(alpha = alpha1 * 0.15f),
                                    animatedGlowColor1.copy(alpha = alpha1 * 0.05f),
                                    Color.Transparent,
                                )

                            val shaderBrush1 =
                                Brush.radialGradient(
                                    0.0f to colors1[0],
                                    0.15f to colors1[1],
                                    0.35f to colors1[2],
                                    0.55f to colors1[3],
                                    0.75f to colors1[4],
                                    0.90f to colors1[5],
                                    1.0f to colors1[6],
                                    center = center1,
                                    radius = radius1,
                                ) as ShaderBrush

                            paint1.shader = shaderBrush1.createShader(size)
                            canvas.drawCircle(center1, radius1, paint1)

                            // 绘制极光 2 (偏右下漂移，限制在右下象限活动，避免越界和被遮盖)
                            val center2 =
                                Offset(
                                    x = w * 0.75f + w * (glow2X * 0.35f),
                                    y = h * 0.75f + h * (glow2Y * 0.35f),
                                )
                            val radius2 = baseRadius * glow2RadiusScale

                            val paint2 =
                                Paint().apply {
                                    isAntiAlias = true
                                    asFrameworkPaint().isDither = true // 开启硬件级混色抖动
                                }

                            val alpha2 = 0.35f * coverAlpha.value
                            val colors2 =
                                listOf(
                                    animatedGlowColor2.copy(alpha = alpha2),
                                    animatedGlowColor2.copy(alpha = alpha2 * 0.8f),
                                    animatedGlowColor2.copy(alpha = alpha2 * 0.55f),
                                    animatedGlowColor2.copy(alpha = alpha2 * 0.32f),
                                    animatedGlowColor2.copy(alpha = alpha2 * 0.15f),
                                    animatedGlowColor2.copy(alpha = alpha2 * 0.05f),
                                    Color.Transparent,
                                )

                            val shaderBrush2 =
                                Brush.radialGradient(
                                    0.0f to colors2[0],
                                    0.15f to colors2[1],
                                    0.35f to colors2[2],
                                    0.55f to colors2[3],
                                    0.75f to colors2[4],
                                    0.90f to colors2[5],
                                    1.0f to colors2[6],
                                    center = center2,
                                    radius = radius2,
                                ) as ShaderBrush

                            paint2.shader = shaderBrush2.createShader(size)
                            canvas.drawCircle(center2, radius2, paint2)
                        }

                        // 绘制背景极光之后，再绘制 Box 内部的主体前景内容
                        drawContent()
                    }
                }
                .then(dragModifier)
    ) {

        // 精心调配的多重渐变遮罩，使封面色彩流淌的同时保证文字的可读性
        val overlayBrush =
            if (isDarkMode) {
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color(0xCC0C0C0E), // 顶部暗色，适合状态栏 and 操作按钮
                            Color(0x550C0C0E), // 中间极透明，展现封面中心色彩
                            Color(0xEE0C0C0E), // 底部暗色，适合播放器控制盘 and 歌词底色
                        )
                )
            } else {
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color(0xCCF5F5F7), // 顶部
                            Color(0x55F5F5F7), // 中间
                            Color(0xEEF5F5F7), // 底部
                        )
                )
            }

        val radialOverlayBrush =
            Brush.radialGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        if (isDarkMode) Color(0x660C0C0E) else Color(0x66F5F5F7),
                    )
            )

        Box(modifier = Modifier.fillMaxSize().background(overlayBrush))
        Box(modifier = Modifier.fillMaxSize().background(radialOverlayBrush))

        // Main content
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // 左侧主播放器内容：占一半宽度，高度撑满，和竖屏样式一致，但去除 HorizontalPager，并使用较小的 padding
                Column(
                    modifier = Modifier.weight(1.0f).fillMaxHeight().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlayerTopBar(
                        song = song,
                        isPlaying = isPlaying,
                        isDarkMode = isDarkMode,
                        appColors = appColors,
                        currentAccent = currentAccent,
                        onDismiss = onDismiss,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onNavigateToArtist = onNavigateToArtist,
                        onSleepTimerClick = onSleepTimerClick,
                    )

                    val shouldRotate = isPlaying && isCoverLoaded
                    LaunchedEffect(shouldRotate) {
                        if (shouldRotate) {
                            val startFrameTime = android.os.SystemClock.uptimeMillis()
                            val startAngle = rotationAngle
                            while (true) {
                                withFrameMillis { frameTime ->
                                    val elapsed =
                                        android.os.SystemClock.uptimeMillis() - startFrameTime
                                    rotationAngle = (startAngle + elapsed * 0.024f) % 360f
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        VinylDisc(
                            isPlaying = isPlaying,
                            isCoverLoaded = isCoverLoaded,
                            bgBitmap = bgBitmap,
                            currentAccent = currentAccent,
                            cardScale = cardScale,
                            rotationAngle = { rotationAngle },
                            coverAlpha = coverAlpha.value,
                            size = 250.dp, // 设定横屏黄金尺寸 195.dp，您可以直接修改此数值
                            onTogglePlay = {
                                if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SongMetadata(
                        song = song,
                        isDarkMode = isDarkMode,
                        appColors = appColors,
                        favoriteColor = favoriteColor,
                        onAddToBlacklist = {
                            if (song.isBlacklisted) {
                                viewModel.removeFromBlacklist(song)
                                Toast.makeText(context, "已从遗忘的沙漏恢复", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addToBlacklist(context, song)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(song, skipListUpdate = true) },
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PlayerProgressBar(
                        song = song,
                        playbackProgress = playbackProgress,
                        isDarkMode = isDarkMode,
                        currentAccent = currentAccent,
                        appColors = appColors,
                        onSeek = { viewModel.seekTo(it) },
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PlaybackControllers(
                        isPlaying = isPlaying,
                        playMode = playMode,
                        currentAccent = currentAccent,
                        appColors = appColors,
                        onTogglePlayMode = {
                            viewModel.togglePlayMode()
                            val modeStr =
                                when (playMode) {
                                    PlayMode.ListLoop -> "单曲循环"
                                    PlayMode.SingleLoop -> "随机播放"
                                    PlayMode.Shuffle -> "列表循环"
                                }
                            Toast.makeText(context, "播放模式已切至: $modeStr", Toast.LENGTH_SHORT).show()
                        },
                        onPlayPrevious = { viewModel.playPreviousSong(context) },
                        onTogglePlay = {
                            if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                        },
                        onPlayNext = { viewModel.playNextSong(context) },
                        onShowQueue = { showPlaybackQueueDialog = true },
                    )

                    Spacer(modifier = Modifier.navigationBarsPadding().height(8.dp))
                }

                // 右侧歌词：占一半宽度，高度撑满，增加 statusBarsPadding / navigationBarsPadding 适配
                Box(
                    modifier =
                        Modifier.weight(1.0f)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LyricView(
                        song = song,
                        playbackProgress = progressProvider,
                        appColors = appColors,
                        currentAccent = currentAccent,
                        onSeek = { position -> viewModel.seekTo(position) },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerTopBar(
                    song = song,
                    isPlaying = isPlaying,
                    isDarkMode = isDarkMode,
                    appColors = appColors,
                    currentAccent = currentAccent,
                    onDismiss = onDismiss,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onNavigateToArtist = onNavigateToArtist,
                    onSleepTimerClick = onSleepTimerClick,
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1.2f).fillMaxWidth(),
                ) { page ->
                    if (page == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            val shouldRotate = isPlaying && isCoverLoaded
                            LaunchedEffect(shouldRotate) {
                                if (shouldRotate) {
                                    val startFrameTime = android.os.SystemClock.uptimeMillis()
                                    val startAngle = rotationAngle
                                    while (true) {
                                        withFrameMillis { frameTime ->
                                            val elapsed =
                                                android.os.SystemClock.uptimeMillis() -
                                                    startFrameTime
                                            rotationAngle = (startAngle + elapsed * 0.024f) % 360f
                                        }
                                    }
                                }
                            }

                            VinylDisc(
                                isPlaying = isPlaying,
                                isCoverLoaded = isCoverLoaded,
                                bgBitmap = bgBitmap,
                                currentAccent = currentAccent,
                                cardScale = cardScale,
                                rotationAngle = { rotationAngle },
                                coverAlpha = coverAlpha.value,
                                size = 300.dp,
                                onTogglePlay = {
                                    if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                                },
                            )
                        }
                    } else {
                        LyricView(
                            song = song,
                            playbackProgress = progressProvider,
                            appColors = appColors,
                            currentAccent = currentAccent,
                            onSeek = { position -> viewModel.seekTo(position) },
                            isPageActive = pagerState.currentPage == 1,
                        )
                    }
                }

                SongMetadata(
                    song = song,
                    isDarkMode = isDarkMode,
                    appColors = appColors,
                    favoriteColor = favoriteColor,
                    onAddToBlacklist = {
                        if (song.isBlacklisted) {
                            viewModel.removeFromBlacklist(song)
                            Toast.makeText(context, "已从遗忘的沙漏恢复", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addToBlacklist(context, song)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(song, skipListUpdate = true) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlayerProgressBar(
                    song = song,
                    playbackProgress = playbackProgress,
                    isDarkMode = isDarkMode,
                    currentAccent = currentAccent,
                    appColors = appColors,
                    onSeek = { viewModel.seekTo(it) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                PlaybackControllers(
                    isPlaying = isPlaying,
                    playMode = playMode,
                    currentAccent = currentAccent,
                    appColors = appColors,
                    onTogglePlayMode = {
                        viewModel.togglePlayMode()
                        val modeStr =
                            when (playMode) {
                                PlayMode.ListLoop -> "单曲循环"
                                PlayMode.SingleLoop -> "随机播放"
                                PlayMode.Shuffle -> "列表循环"
                            }
                        Toast.makeText(context, "播放模式已切至: $modeStr", Toast.LENGTH_SHORT).show()
                    },
                    onPlayPrevious = { viewModel.playPreviousSong(context) },
                    onTogglePlay = {
                        if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                    },
                    onPlayNext = { viewModel.playNextSong(context) },
                    onShowQueue = { showPlaybackQueueDialog = true },
                )

                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }

    if (showPlaybackQueueDialog) {
        val qItemHighlightBg = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0D000000)
        val queueListState = rememberLazyListState()

        LaunchedEffect(Unit) {
            val currentIndex = playbackQueue.indexOfFirst { it.id == song.id }
            if (currentIndex != -1) {
                queueListState.scrollToItem(currentIndex)
            }
        }

        AppDialog(
            onDismissRequest = { showPlaybackQueueDialog = false },
            title = "当前播放列表",
            icon = Icons.Default.QueueMusic,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                Text(
                    text = "${playbackQueue.size} 首",
                    color = appColors.textColorSecondary.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            },
        ) {
            if (playbackQueue.isEmpty()) {
                Text(text = "播放列表为空", color = appColors.textColorSecondary, fontSize = 13.sp)
            } else {
                LazyColumn(
                    state = queueListState,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                ) {
                    items(playbackQueue) { qSong ->
                        val isCurrent = qSong.id == song.id
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { viewModel.playSong(context, qSong) }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrent) qItemHighlightBg else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = qSong.title,
                                    color =
                                        if (isCurrent) currentAccent.mainColor
                                        else appColors.textColorPrimary,
                                    fontWeight =
                                        if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = qSong.artist,
                                    color = appColors.textColorSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.removeFromPlaybackQueue(qSong.id, context)
                                    Toast.makeText(context, "已从播放列表移出", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "移出播放列表",
                                    tint = appColors.textColorSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerTopBar(
    song: Song,
    isPlaying: Boolean,
    isDarkMode: Boolean,
    appColors: AppColors,
    currentAccent: AccentColor,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.size(48.dp))

        Text(
            text = if (isPlaying) "正在播放" else "已暂停",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textColorPrimary.copy(alpha = 0.9f),
            letterSpacing = 0.5.sp,
        )

        Box {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多操作",
                    tint = appColors.textColorPrimary,
                )
            }
            if (showMoreMenu) {
                androidx.compose.ui.window.Popup(
                    onDismissRequest = { showMoreMenu = false },
                    alignment = Alignment.TopEnd,
                    offset =
                        androidx.compose.ui.unit.IntOffset(
                            0,
                            with(androidx.compose.ui.platform.LocalDensity.current) {
                                44.dp.roundToPx()
                            },
                        ),
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                ) {
                    Box(
                        modifier =
                            Modifier.width(IntrinsicSize.Max)
                                .widthIn(max = 180.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(appColors.surfaceColor)
                                .border(
                                    0.5.dp,
                                    if (isDarkMode) Color.White.copy(alpha = 0.12f)
                                    else Color.Black.copy(alpha = 0.08f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(vertical = 4.dp)
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "加入到歌单",
                                        color = appColors.textColorPrimary,
                                        fontSize = 14.sp,
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
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.requiredHeight(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "歌曲详情",
                                        color = appColors.textColorPrimary,
                                        fontSize = 14.sp,
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
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.requiredHeight(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "定时关闭",
                                        color = appColors.textColorPrimary,
                                        fontSize = 14.sp,
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
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.requiredHeight(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "音乐标签编辑",
                                        color = appColors.textColorPrimary,
                                        fontSize = 14.sp,
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    openInMusicTagEditor(context, song.path)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = appColors.textColorPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.requiredHeight(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            )
                            if (song.artist.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "歌手：${song.artist}",
                                            color = appColors.textColorPrimary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
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
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    modifier = Modifier.requiredHeight(32.dp),
                                    contentPadding =
                                        PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                )
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

        AppDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = "歌曲详情",
            icon = Icons.Default.Info,
            iconColor = currentAccent.mainColor,
            appColors = appColors,
            actionArea = {
                IconButton(
                    onClick = { showDetailsDialog = false },
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DialogItemSpacing),
            ) {
                DetailRow(label = "歌名", value = song.title, appColors = appColors)
                DetailRow(
                    label = "歌手",
                    value = if (song.artist.isBlank()) "未知歌手" else song.artist,
                    appColors = appColors,
                )
                DetailRow(
                    label = "专辑",
                    value = if (song.album.isBlank()) "未知专辑" else song.album,
                    appColors = appColors,
                )
                DetailRow(
                    label = "时长",
                    value = formatDuration(song.duration),
                    appColors = appColors,
                )
                DetailRow(label = "大小", value = sizeText, appColors = appColors)
                DetailRow(label = "文件路径", value = song.path, appColors = appColors, isPath = true)
            }
        }
    }
}

@Composable
fun VinylDisc(
    isPlaying: Boolean,
    isCoverLoaded: Boolean,
    bgBitmap: android.graphics.Bitmap?,
    currentAccent: AccentColor,
    cardScale: Float,
    rotationAngle: () -> Float,
    coverAlpha: Float,
    size: Dp,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverDp = size * 0.66f

    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    rotationZ = rotationAngle()
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onTogglePlay()
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2C2C2E), Color(0xFF151517), Color(0xFF0C0C0E))
                    )
                )
                .border(4.dp, Color(0xFF232325), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val ringCount = 8
            val startRadius = size.toPx() * 0.34f
            val endRadius = size.toPx() * 0.473f
            for (i in 0 until ringCount) {
                val r = startRadius + (endRadius - startRadius) * (i.toFloat() / (ringCount - 1))
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1f),
                )
            }
        }

        Box(
            modifier =
                Modifier.size(coverDp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .graphicsLayer { alpha = coverAlpha }
        ) {
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap.asImageBitmap(),
                    contentDescription = "Cover Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            currentAccent.mainColor.copy(alpha = 0.8f),
                                            currentAccent.mainColor.copy(alpha = 0.4f),
                                        )
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(size * 0.12f),
                    )
                }
            }
        }
    }
}

@Composable
fun SongMetadata(
    song: Song,
    isDarkMode: Boolean,
    appColors: AppColors,
    favoriteColor: Color,
    onAddToBlacklist: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onAddToBlacklist,
            modifier =
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDarkMode) Color.White.copy(alpha = 0.06f)
                        else Color.Black.copy(alpha = 0.04f)
                    ),
        ) {
            Icon(
                imageVector =
                    if (song.isBlacklisted) Icons.Default.Refresh else Icons.Default.HourglassEmpty,
                contentDescription = if (song.isBlacklisted) "从遗忘的沙漏恢复" else "遗忘的沙漏",
                tint =
                    if (song.isBlacklisted) appColors.textColorSecondary.copy(alpha = 0.6f)
                    else Color(0xFFE5C07B),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1.0f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = song.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textColorPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val quality = song.getQualityType()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (song.isBlacklisted) {
            Spacer(modifier = Modifier.size(40.dp))
        } else {
            IconButton(
                onClick = onToggleFavorite,
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkMode) Color.White.copy(alpha = 0.06f)
                            else Color.Black.copy(alpha = 0.04f)
                        ),
            ) {
                Icon(
                    imageVector =
                        if (song.isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                    contentDescription = "喜欢",
                    tint = favoriteColor,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
fun PlayerProgressBar(
    song: Song,
    playbackProgress: Long,
    isDarkMode: Boolean,
    currentAccent: AccentColor,
    appColors: AppColors,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    val currentPos = sliderPosition?.toLong() ?: playbackProgress
    val progressPercent =
        if (song.duration > 0) currentPos.toFloat() / song.duration.toFloat() else 0f
    var progressBarWidth by remember { mutableStateOf(1f) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(26.dp)
                    .onGloballyPositioned { progressBarWidth = it.size.width.toFloat() }
                    .pointerInput(song.duration) {
                        detectTapGestures(
                            onPress = { offset ->
                                val fraction = (offset.x / progressBarWidth).coerceIn(0f, 1f)
                                sliderPosition = fraction * song.duration
                                tryAwaitRelease()
                                sliderPosition?.let { onSeek(it.toLong()) }
                                sliderPosition = null
                            }
                        )
                    }
                    .pointerInput(song.duration) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val fraction = (offset.x / progressBarWidth).coerceIn(0f, 1f)
                                sliderPosition = fraction * song.duration
                            },
                            onDrag = { change, _ ->
                                val fraction =
                                    (change.position.x / progressBarWidth).coerceIn(0f, 1f)
                                sliderPosition = fraction * song.duration
                            },
                            onDragEnd = {
                                sliderPosition?.let { onSeek(it.toLong()) }
                                sliderPosition = null
                            },
                            onDragCancel = { sliderPosition = null },
                        )
                    },
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackColor =
                if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
            val activeColor = currentAccent.mainColor

            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                val width = size.width
                val height = size.height
                val cap = StrokeCap.Round

                drawLine(
                    color = trackColor,
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = height,
                    cap = cap,
                )

                drawLine(
                    color = activeColor,
                    start = Offset(0f, height / 2),
                    end = Offset(width * progressPercent, height / 2),
                    strokeWidth = height,
                    cap = cap,
                )
            }

            val thumbOffset = progressBarWidth * progressPercent
            Box(
                modifier =
                    Modifier.graphicsLayer {
                            translationX = (thumbOffset - 5.dp.toPx()).coerceAtLeast(0f)
                        }
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                        .border(0.5.dp, currentAccent.mainColor.copy(alpha = 0.3f), CircleShape)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentPos),
                color = appColors.textColorSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatDuration(song.duration),
                color = appColors.textColorSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun PlaybackControllers(
    isPlaying: Boolean,
    playMode: PlayMode,
    currentAccent: AccentColor,
    appColors: AppColors,
    onTogglePlayMode: () -> Unit,
    onPlayPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playBtnIconColor = Color.White

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onTogglePlayMode, modifier = Modifier.size(44.dp)) {
            val modeIcon =
                when (playMode) {
                    PlayMode.ListLoop -> Icons.Default.Repeat
                    PlayMode.SingleLoop -> Icons.Default.RepeatOne
                    PlayMode.Shuffle -> Icons.Default.Shuffle
                }
            Icon(
                imageVector = modeIcon,
                contentDescription = "播放模式",
                tint = appColors.textColorPrimary.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = onPlayPrevious, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一首",
                    tint = appColors.textColorPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(30.dp),
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                Box(
                    modifier =
                        Modifier.size(56.dp)
                            .background(
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            currentAccent.mainColor.copy(alpha = 0.4f),
                                            Color.Transparent,
                                        )
                                )
                            )
                            .blur(8.dp)
                )
                Box(
                    modifier =
                        Modifier.size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            currentAccent.mainColor,
                                            currentAccent.mainColor.copy(alpha = 0.85f),
                                        )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector =
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放暂停",
                        tint = playBtnIconColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            IconButton(onClick = onPlayNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一首",
                    tint = appColors.textColorPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        IconButton(onClick = onShowQueue, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = "播放列表",
                tint = appColors.textColorPrimary.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 跳转至「音乐标签」编辑该音频文件 */
internal fun openInMusicTagEditor(context: Context, audioFilePath: String) {
    val file = File(audioFilePath)
    if (!file.exists()) {
        Toast.makeText(context, "音乐文件不存在", Toast.LENGTH_SHORT).show()
        return
    }

    // 1. 判断是否安装「音乐标签」应用
    val isAppInstalled =
        try {
            context.packageManager.getPackageInfo("com.xjcheng.musictageditor", 0)
            true
        } catch (e: Exception) {
            false
        }

    if (!isAppInstalled) {
        Toast.makeText(context, "未检测到已安装「音乐标签」应用，正在前往下载页面...", Toast.LENGTH_SHORT).show()
        try {
            val downloadIntent =
                Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.cnblogs.com/vinlxc/p/11932130.html"),
                    )
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(downloadIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开下载页面", Toast.LENGTH_SHORT).show()
        }
        return
    }

    // 2. 如果已安装，生成 file:// 类型的 Uri 并尝试用 ACTION_EDIT 唤起
    // 提示：音乐标签必须直接修改物理文件以完成保存，使用 file:// 可以避免其因 content:// 转化为临时文件 tempFile 导致无法保存和文件名丢失的问题。
    try {
        // 绕过 StrictMode FileUriExposedException 限制
        val builder = android.os.StrictMode.VmPolicy.Builder()
        android.os.StrictMode.setVmPolicy(builder.build())

        val fileUri: Uri = Uri.fromFile(file)

        val editIntent =
            Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(fileUri, "audio/*")
                setPackage("com.xjcheng.musictageditor")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        try {
            context.startActivity(editIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            // 3. 如果 ACTION_EDIT 报错，尝试用 ACTION_VIEW 唤起
            val viewIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "audio/*")
                    setPackage("com.xjcheng.musictageditor")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(viewIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "打开失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
