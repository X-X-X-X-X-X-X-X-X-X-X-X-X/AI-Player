package cn.xuexc.ai_player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
val DialogMaxWidth = 400.dp
val DialogModifier =
    Modifier.widthIn(max = DialogMaxWidth)
        .fillMaxWidth()
        .padding(horizontal = DialogHorizontalPadding)

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color = iconColor.copy(alpha = 0.12f),
    titleColor: Color = Color.Unspecified,
    appColors: AppColors,
    actionArea: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier =
                DialogModifier.border(
                    width = 0.5.dp,
                    color =
                        if (appColors.surfaceColor == Color(0xFF161619))
                            Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.08f),
                    shape = DialogShape,
                ),
            shape = DialogShape,
            colors = CardDefaults.cardColors(containerColor = appColors.surfaceColor),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(DialogInnerPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(iconBgColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Text(
                            text = title,
                            color =
                                if (titleColor != Color.Unspecified) titleColor
                                else appColors.navBarItemActive,
                            fontSize = DialogTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (actionArea != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            actionArea()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DialogTitleToContentSpace))

                content()
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, appColors: AppColors, isPath: Boolean = false) {
    if (isPath) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, fontSize = 11.sp, color = appColors.textColorSecondary)
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                color = appColors.textColorPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = appColors.textColorSecondary,
                modifier = Modifier.width(48.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = appColors.textColorPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
}
