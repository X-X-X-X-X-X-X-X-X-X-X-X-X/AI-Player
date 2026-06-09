package cn.xuexc.ai_player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
