package cn.xuexc.ai_player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommonItemCard(
    cover: @Composable () -> Unit,
    title: String,
    subtitle: @Composable () -> Unit,
    appColors: AppColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    actionArea: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDarkMode = appColors.surfaceColor == Color(0xFF161619)
    val pressedBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    
    val cardBg = containerColor ?: if (isPressed) pressedBgColor else appColors.cardBackground

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cover()

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textColorPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                subtitle()
            }

            if (actionArea != null) {
                Spacer(modifier = Modifier.width(2.dp))
                actionArea()
            }
        }
    }
}

@Composable
fun CommonItemCard(
    cover: @Composable () -> Unit,
    title: String,
    subtitleText: String,
    appColors: AppColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    actionArea: @Composable (() -> Unit)? = null
) {
    CommonItemCard(
        cover = cover,
        title = title,
        subtitle = {
            Text(
                text = subtitleText,
                fontSize = 11.sp,
                color = appColors.textColorSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        appColors = appColors,
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        actionArea = actionArea
    )
}
