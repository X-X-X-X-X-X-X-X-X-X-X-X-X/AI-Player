package cn.xuexc.ai_player.data

import android.icu.text.Transliterator
import java.util.concurrent.ConcurrentHashMap

data class SongItemWithLetter(
    val song: Song,
    val letter: Char
)

data class ArtistItem(
    val artistKey: String,
    val displayName: String,
    val songs: List<Song>,
    val letter: Char
)

object PinyinHelper {
    // 字符级别拼音首字母缓存，极大减少 Transliterator 的调用次数
    private val charLetterCache = ConcurrentHashMap<Char, Char>()

    // 单例持有 Transliterator，避免重复创建的高额开销
    private object TransliteratorHolder {
        val transliterator: Transliterator by lazy {
            Transliterator.getInstance("Any-Latin; Latin-ASCII; Upper")
        }
    }

    fun getArtistLetter(artist: String): Char {
        if (artist.isBlank()) return '#'
        val firstChar = artist.trim().first()

        // 快速通道：如果是英文字符，直接返回其大写，无需查缓存或做拼音转换
        if (firstChar in 'a'..'z' || firstChar in 'A'..'Z') {
            return firstChar.uppercaseChar()
        }

        // 快速通道：检查缓存
        val cached = charLetterCache[firstChar]
        if (cached != null) return cached

        val result = try {
            // 使用单例 Transliterator 进行转换
            val pinyin = TransliteratorHolder.transliterator.transliterate(firstChar.toString())
            val firstPinyinChar = pinyin.firstOrNull() ?: '#'
            if (firstPinyinChar in 'a'..'z' || firstPinyinChar in 'A'..'Z') {
                firstPinyinChar.uppercaseChar()
            } else {
                '#'
            }
        } catch (e: Exception) {
            '#'
        }

        charLetterCache[firstChar] = result
        return result
    }
}
