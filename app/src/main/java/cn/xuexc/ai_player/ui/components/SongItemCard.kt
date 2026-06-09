package cn.xuexc.ai_player.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.data.QualityType
import cn.xuexc.ai_player.data.Song
import cn.xuexc.ai_player.data.getQualityType
import java.util.*

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
    onDelete: (() -> Unit)? = null,
    inSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = inSelectionMode,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) appColors.navBarItemActive else appColors.textColorSecondary.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(if (isSelected) appColors.navBarItemActive else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSelectionChange?.invoke(!isSelected)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = appColors.surfaceColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        CommonItemCard(
            modifier = Modifier.weight(1.0f),
            cover = {
                // Album Art or LiveVisualizer (slightly downscaled to 42.dp for denser presentation)
                SongCover(
                    song = song, isCurrent = isCurrent, isPlaying = isPlaying, modifier = Modifier.size(42.dp)
                )
            },
            title = song.title,
            subtitle = {
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
            },
            appColors = appColors,
            onClick = {
                if (inSelectionMode) {
                    onSelectionChange?.invoke(!isSelected)
                } else {
                    onClick()
                }
            },
            onLongClick = {
                if (!inSelectionMode) {
                    onLongClick?.invoke()
                }
            },
            containerColor = cardBg,
            actionArea = {
                if (!inSelectionMode) {
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
                                                        text = label,
                                                        color = appColors.textColorPrimary,
                                                        fontSize = 14.sp
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
                                                        text = "移至遗忘的沙漏",
                                                        color = Color(0xFFE5C07B),
                                                        fontSize = 14.sp
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
                                                    text = "歌曲详情",
                                                    color = appColors.textColorPrimary,
                                                    fontSize = 14.sp
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
                                                text = {
                                                    Text(
                                                        text = "删除歌曲",
                                                        color = Color(0xFFE06C75),
                                                        fontSize = 14.sp
                                                    )
                                                },
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
        )
    }

    if (showDetailsDialog) {
        val sizeMb = song.size / (1024f * 1024f)
        val sizeText = String.format(Locale.getDefault(), "%.2f MB", sizeMb)

        AppDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = "歌曲详情",
            icon = Icons.Default.Info,
            iconColor = appColors.navBarItemActive,
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
