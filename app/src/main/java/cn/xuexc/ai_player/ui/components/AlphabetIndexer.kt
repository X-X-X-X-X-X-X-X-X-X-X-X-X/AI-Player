package cn.xuexc.ai_player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
