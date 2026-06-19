package cn.xuexc.ai_player.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.xuexc.ai_player.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SongCover(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.MusicNote,
) {
    val context = LocalContext.current
    var bitmap by remember(song.id, song.lastModified) { mutableStateOf(song.getCachedCover()) }

    LaunchedEffect(song.id, song.lastModified) {
        if (bitmap == null) {
            if (!song.hasDiskCover(context, 150)) {
                delay(80)
            }
            withContext(Dispatchers.IO) { bitmap = song.loadCover(context, 150) }
        }
    }

    Box(
        modifier = modifier.clip(shape).background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Overlay visualizer or play icon
        if (isCurrent) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else if (bitmap == null) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = "Cover Placeholder",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun PlaylistCover(
    firstSongId: Long?,
    currentAccent: AccentColor,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val context = LocalContext.current
    var bitmap by
        remember(firstSongId) { mutableStateOf(firstSongId?.let { getCachedCoverById(it) }) }

    LaunchedEffect(firstSongId) {
        if (firstSongId != null) {
            if (bitmap == null) {
                if (!hasDiskCoverById(context, firstSongId, 150)) {
                    delay(100)
                }
                withContext(Dispatchers.IO) { bitmap = loadCoverById(context, firstSongId, 150) }
            }
        } else {
            bitmap = null
        }
    }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(if (bitmap != null) Color(0xFF2C2C2E) else currentAccent.mainColor),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Playlist Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun MiniPlayerCover(
    modifier: Modifier = Modifier,
    song: Song,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf(song.getCachedCover()) }

    LaunchedEffect(song.id, song.lastModified) {
        val loaded = withContext(Dispatchers.IO) { song.loadCover(context, 150) }
        bitmap = loaded
    }

    Box(
        modifier = modifier.clip(shape).background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Mini Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Cover Placeholder",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
