package cn.xuexc.ai_player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.ui.ScanStatus

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
