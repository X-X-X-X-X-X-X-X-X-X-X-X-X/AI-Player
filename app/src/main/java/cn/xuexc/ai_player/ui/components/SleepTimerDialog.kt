package cn.xuexc.ai_player.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.ui.SongViewModel

@Composable
fun SleepTimerDialog(
    viewModel: SongViewModel,
    appColors: AppColors,
    isDarkMode: Boolean,
    currentAccent: AccentColor,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val remainingMs by viewModel.sleepTimerRemaining.collectAsState()
    val playComplete by viewModel.sleepTimerPlayComplete.collectAsState()

    var localPlayComplete by remember { mutableStateOf(playComplete) }
    var customMinutes by remember { mutableStateOf(30f) }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = "定时关闭",
        icon = Icons.Default.AccessTime,
        iconColor = currentAccent.mainColor,
        appColors = appColors,
        actionArea = {
            IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "关闭",
                    tint = appColors.textColorSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (remainingMs > 0L) currentAccent.mainColor.copy(alpha = 0.1f)
                        else if (isDarkMode) Color.White.copy(alpha = 0.04f)
                        else Color.Black.copy(alpha = 0.02f)
                    )
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Crossfade(targetState = remainingMs > 0L, label = "sleep_timer_state_crossfade") {
                isRunning ->
                if (isRunning) {
                    val minutes = remainingMs / 1000 / 60
                    val seconds = (remainingMs / 1000) % 60
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "倒计时中：${minutes}分${seconds}秒",
                            color = appColors.textColorPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "取消定时",
                            color = currentAccent.mainColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.clickable {
                                    viewModel.cancelSleepTimer()
                                    android.widget.Toast.makeText(
                                            context,
                                            "定时关闭已取消",
                                            android.widget.Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "定时关闭未开启",
                            color = appColors.textColorSecondary,
                            fontSize = 13.sp,
                        )
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = appColors.textColorSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (isDarkMode) Color.White.copy(alpha = 0.1f)
                        else Color.Black.copy(alpha = 0.05f)
                    )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "自定义定时时间", color = appColors.textColorSecondary, fontSize = 11.sp)
                Text(
                    text = "${customMinutes.toInt()} 分钟",
                    color = currentAccent.mainColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = customMinutes,
                onValueChange = { customMinutes = it },
                valueRange = 1f..120f,
                colors =
                    SliderDefaults.colors(
                        thumbColor = currentAccent.mainColor,
                        activeTrackColor = currentAccent.mainColor,
                        inactiveTrackColor =
                            if (isDarkMode) Color.White.copy(alpha = 0.1f)
                            else Color.Black.copy(alpha = 0.05f),
                    ),
            )
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { localPlayComplete = !localPlayComplete }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "播放完整首再关闭",
                    color = appColors.textColorPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Switch(
                    checked = localPlayComplete,
                    onCheckedChange = { localPlayComplete = it },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = currentAccent.mainColor,
                            uncheckedThumbColor = appColors.textColorSecondary,
                            uncheckedTrackColor =
                                if (isDarkMode) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.05f),
                        ),
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = 0.85f
                            scaleY = 0.85f
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        },
                )
            }
            Button(
                onClick = {
                    viewModel.startSleepTimer(
                        customMinutes.toLong() * 60 * 1000L,
                        localPlayComplete,
                    )
                    onDismissRequest()
                    android.widget.Toast.makeText(
                            context,
                            "已设置 ${customMinutes.toInt()} 分钟后关闭",
                            android.widget.Toast.LENGTH_SHORT,
                        )
                        .show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = currentAccent.mainColor),
                shape = DialogButtonShape,
                modifier = Modifier.fillMaxWidth().height(DialogButtonHeight),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("开启自定义定时", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}
