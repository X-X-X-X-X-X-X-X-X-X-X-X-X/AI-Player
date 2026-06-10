package cn.xuexc.ai_player.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun AlphabetIndexer(
    modifier: Modifier = Modifier,
    selectedLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    appColors: AppColors,
    currentAccent: AccentColor
) {
    val alphabet = remember { ('A'..'Z').toList() + '#' }
    var alphabetHeight by remember { mutableStateOf(1f) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val fontSize = if (isLandscape) 8.sp else 10.sp

    Column(
        modifier = modifier
            .fillMaxHeight(0.9f)
            .onGloballyPositioned {
                alphabetHeight = it.size.height.toFloat()
            }
            .pointerInput(alphabet) {
                detectTapGestures(
                    onPress = { offset ->
                        val itemHeight = alphabetHeight / alphabet.size
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                        val letter = alphabet[index]
                        onLetterSelected(letter)
                        onDragStart()
                        tryAwaitRelease()
                        onDragEnd()
                    }
                )
            }
            .pointerInput(alphabet) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val itemHeight = alphabetHeight / alphabet.size
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                        val letter = alphabet[index]
                        onLetterSelected(letter)
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        val itemHeight = alphabetHeight / alphabet.size
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                        val letter = alphabet[index]
                        onLetterSelected(letter)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        alphabet.forEachIndexed { index, letter ->
            val isSelected = selectedLetter == letter
            val showAsDot = isLandscape && (index % 2 != 0) // 横屏下奇数索引显示为圆点占位

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showAsDot) {
                    Text(
                        text = "•",
                        fontSize = fontSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) currentAccent.mainColor else appColors.textColorSecondary.copy(
                            alpha = 0.4f
                        )
                    )
                } else {
                    Text(
                        text = letter.toString(),
                        fontSize = fontSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) currentAccent.mainColor else appColors.textColorSecondary.copy(
                            alpha = 0.6f
                        )
                    )
                }
            }
        }
    }
}
