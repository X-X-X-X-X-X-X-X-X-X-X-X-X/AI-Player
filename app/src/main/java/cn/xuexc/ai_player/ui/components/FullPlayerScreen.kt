package cn.xuexc.ai_player.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.data.QualityType
import cn.xuexc.ai_player.data.Song
import cn.xuexc.ai_player.data.getQualityType
import cn.xuexc.ai_player.data.loadCover
import cn.xuexc.ai_player.data.getCachedCover
import cn.xuexc.ai_player.playback.PlayMode
import cn.xuexc.ai_player.ui.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

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
                    Color(0xCC0C0C0E), // 顶部暗色，适合状态栏 and 操作按钮
                    Color(0x550C0C0E), // 中间极透明，展现封面中心色彩
                    Color(0xEE0C0C0E)  // 底部暗色，适合播放器控制盘 and 歌词底色
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
                                        style = Stroke(width = 1f)
                                    )
                                }
                            }

                            // Center album cover image
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

                    // Elegant micro floating Thumb
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

            // 5. Playback Controller Buttons
            val playBtnIconColor = Color.White

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Play Mode button
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

                // 2. Control Group
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

                // 3. Playback Queue button
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
                    fontWeight = FontWeight.Normal
                )
            }
        ) {
            if (playbackQueue.isEmpty()) {
                Text(
                    text = "播放列表为空", color = appColors.textColorSecondary, fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    state = queueListState,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
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
        ) {
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
