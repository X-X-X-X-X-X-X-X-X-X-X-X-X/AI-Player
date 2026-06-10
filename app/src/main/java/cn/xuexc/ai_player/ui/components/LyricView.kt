package cn.xuexc.ai_player.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xuexc.ai_player.data.Song
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LyricLine(val timeMs: Long, val text: String)

fun parseLrc(lrcContent: String): List<LyricLine> {
    val lines = lrcContent.split("\n")
    val lyricList = mutableListOf<LyricLine>()
    val timeRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
    for (line in lines) {
        val trimmed = line.trim()
        val matches = timeRegex.findAll(trimmed).toList()
        if (matches.isNotEmpty()) {
            val text = trimmed.replace(timeRegex, "").trim()
            if (text.isNotBlank()) {
                for (match in matches) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val milliStr = match.groupValues[3]
                    var milli = milliStr.toLong()
                    if (milliStr.length == 2) {
                        milli *= 10
                    }
                    val timeMs = min * 60 * 1000 + sec * 1000 + milli
                    lyricList.add(LyricLine(timeMs, text))
                }
            }
        }
    }
    return lyricList.sortedBy { it.timeMs }
}

fun extractLyricsFromMp3(filePath: String): String? {
    val file = File(filePath)
    if (!file.exists()) return null
    var fis: FileInputStream? = null
    try {
        fis = FileInputStream(file)
        val header = ByteArray(10)
        if (fis.read(header) != 10) return null

        if (header[0].toChar() != 'I' || header[1].toChar() != 'D' || header[2].toChar() != '3') {
            return null
        }

        val majorVersion = header[3].toInt()

        val tagSize =
            ((header[6].toInt() and 0x7f) shl 21) or
                ((header[7].toInt() and 0x7f) shl 14) or
                ((header[8].toInt() and 0x7f) shl 7) or
                (header[9].toInt() and 0x7f)

        val maxReadSize = minOf(tagSize, 10 * 1024 * 1024)
        val tagData = ByteArray(maxReadSize)
        var totalRead = 0
        while (totalRead < maxReadSize) {
            val read = fis.read(tagData, totalRead, maxReadSize - totalRead)
            if (read == -1) break
            totalRead += read
        }

        var pos = 0
        while (pos + 10 <= totalRead) {
            if (
                tagData[pos].toInt() == 0 &&
                    tagData[pos + 1].toInt() == 0 &&
                    tagData[pos + 2].toInt() == 0 &&
                    tagData[pos + 3].toInt() == 0
            ) {
                break
            }

            val frameId = String(tagData, pos, 4, Charsets.US_ASCII)

            val frameSize =
                if (majorVersion == 3) {
                    ((tagData[pos + 4].toInt() and 0xff) shl 24) or
                        ((tagData[pos + 5].toInt() and 0xff) shl 16) or
                        ((tagData[pos + 6].toInt() and 0xff) shl 8) or
                        (tagData[pos + 7].toInt() and 0xff)
                } else {
                    ((tagData[pos + 4].toInt() and 0x7f) shl 21) or
                        ((tagData[pos + 5].toInt() and 0x7f) shl 14) or
                        ((tagData[pos + 6].toInt() and 0x7f) shl 7) or
                        (tagData[pos + 7].toInt() and 0xff)
                }

            if (frameSize <= 0 || pos + 10 + frameSize > totalRead) {
                break
            }

            if (frameId == "USLT") {
                val payload = tagData.sliceArray((pos + 10) until (pos + 10 + frameSize))
                if (payload.isNotEmpty()) {
                    val encodingByte = payload[0].toInt() and 0xff
                    var textOffset = 4
                    if (encodingByte == 0 || encodingByte == 3) {
                        while (textOffset < payload.size && payload[textOffset].toInt() != 0) {
                            textOffset++
                        }
                        textOffset += 1
                    } else {
                        while (
                            textOffset + 1 < payload.size &&
                                !(payload[textOffset].toInt() == 0 &&
                                    payload[textOffset + 1].toInt() == 0)
                        ) {
                            textOffset += 2
                        }
                        textOffset += 2
                    }
                    if (textOffset < payload.size) {
                        val charset =
                            when (encodingByte) {
                                0 -> Charsets.ISO_8859_1
                                1 -> Charsets.UTF_16
                                2 -> java.nio.charset.Charset.forName("UTF-16BE")
                                3 -> Charsets.UTF_8
                                else -> Charsets.UTF_8
                            }
                        return String(payload, textOffset, payload.size - textOffset, charset)
                    }
                }
            }

            pos += 10 + frameSize
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {}
    }
    return null
}

fun extractLyricsFromFlac(filePath: String): String? {
    val file = File(filePath)
    if (!file.exists()) return null
    var fis: FileInputStream? = null
    try {
        fis = FileInputStream(file)
        val header = ByteArray(4)
        if (fis.read(header) != 4) return null
        if (
            header[0].toChar() != 'f' ||
                header[1].toChar() != 'L' ||
                header[2].toChar() != 'a' ||
                header[3].toChar() != 'C'
        ) {
            return null
        }

        var isLast = false
        while (!isLast) {
            val blockHeader = ByteArray(4)
            if (fis.read(blockHeader) != 4) break

            isLast = (blockHeader[0].toInt() and 0x80) != 0
            val blockType = blockHeader[0].toInt() and 0x7f
            val blockLength =
                ((blockHeader[1].toInt() and 0xff) shl 16) or
                    ((blockHeader[2].toInt() and 0xff) shl 8) or
                    (blockHeader[3].toInt() and 0xff)

            if (blockType == 4) { // VORBIS_COMMENT
                val blockData = ByteArray(blockLength)
                var totalRead = 0
                while (totalRead < blockLength) {
                    val read = fis.read(blockData, totalRead, blockLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }

                var offset = 0
                fun readInt32(): Int {
                    if (offset + 4 > blockLength) return 0
                    val val32 =
                        (blockData[offset].toInt() and 0xff) or
                            ((blockData[offset + 1].toInt() and 0xff) shl 8) or
                            ((blockData[offset + 2].toInt() and 0xff) shl 16) or
                            ((blockData[offset + 3].toInt() and 0xff) shl 24)
                    offset += 4
                    return val32
                }

                val vendorLength = readInt32()
                offset += vendorLength

                val userCommentListLength = readInt32()
                for (i in 0 until userCommentListLength) {
                    val commentLength = readInt32()
                    if (offset + commentLength > blockLength) break
                    val comment = String(blockData, offset, commentLength, Charsets.UTF_8)
                    offset += commentLength

                    val eqIdx = comment.indexOf('=')
                    if (eqIdx != -1) {
                        val key = comment.substring(0, eqIdx).trim().uppercase()
                        if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "LYRIC") {
                            return comment.substring(eqIdx + 1)
                        }
                    }
                }
                break
            } else {
                fis.skip(blockLength.toLong())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {}
    }
    return null
}

fun extractLyricsFromM4a(filePath: String): String? {
    val file = File(filePath)
    if (!file.exists()) return null
    var fis: FileInputStream? = null
    try {
        fis = FileInputStream(file)
        val fileLength = file.length()

        fun searchAtom(
            startOffset: Long,
            endOffset: Long,
            targetPath: List<String>,
            pathIndex: Int,
        ): String? {
            var offset = startOffset
            while (offset + 8 <= endOffset) {
                fis?.channel?.position(offset)
                val header = ByteArray(8)
                if (fis?.read(header) != 8) break

                val size =
                    ((header[0].toInt() and 0xff) shl 24) or
                        ((header[1].toInt() and 0xff) shl 16) or
                        ((header[2].toInt() and 0xff) shl 8) or
                        (header[3].toInt() and 0xff)
                val type = String(header, 4, 4, Charsets.US_ASCII)
                val actualSize =
                    if (size == 1) {
                        val size64 = ByteArray(8)
                        fis?.read(size64)
                        var sizeVal = 0L
                        for (i in 0..7) {
                            sizeVal = (sizeVal shl 8) or (size64[i].toLong() and 0xff)
                        }
                        sizeVal
                    } else if (size == 0) {
                        endOffset - offset
                    } else {
                        size.toLong()
                    }

                if (actualSize <= 0) break

                val targetType = targetPath[pathIndex]
                if (type == targetType) {
                    if (pathIndex == targetPath.lastIndex) {
                        val contentStart = offset + (if (size == 1) 16 else 8)
                        val contentEnd = offset + actualSize
                        var childOffset = contentStart
                        while (childOffset + 8 <= contentEnd) {
                            fis?.channel?.position(childOffset)
                            val childHeader = ByteArray(8)
                            fis?.read(childHeader)
                            val childSize =
                                ((childHeader[0].toInt() and 0xff) shl 24) or
                                    ((childHeader[1].toInt() and 0xff) shl 16) or
                                    ((childHeader[2].toInt() and 0xff) shl 8) or
                                    (childHeader[3].toInt() and 0xff)
                            val childType = String(childHeader, 4, 4, Charsets.US_ASCII)
                            if (childType == "data") {
                                val dataContentStart = childOffset + 16
                                val dataLength = childSize - 16
                                if (dataLength > 0) {
                                    val dataBytes = ByteArray(dataLength)
                                    fis?.channel?.position(dataContentStart)
                                    fis?.read(dataBytes)
                                    return String(dataBytes, Charsets.UTF_8)
                                }
                            }
                            childOffset += childSize
                        }
                        return null
                    } else {
                        val containerStart =
                            offset + (if (size == 1) 16 else 8) + (if (type == "meta") 4 else 0)
                        val containerEnd = offset + actualSize
                        val result =
                            searchAtom(containerStart, containerEnd, targetPath, pathIndex + 1)
                        if (result != null) return result
                    }
                }
                offset += actualSize
            }
            return null
        }

        val copyrightLyr =
            String(
                byteArrayOf(0xA9.toByte(), 0x6C.toByte(), 0x79.toByte(), 0x72.toByte()),
                Charsets.ISO_8859_1,
            )
        val path = listOf("moov", "udta", "meta", "ilst", copyrightLyr)
        return searchAtom(0L, fileLength, path, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
        } catch (e: Exception) {}
    }
    return null
}

fun extractLyricsFromAudio(filePath: String): String? {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp3" -> extractLyricsFromMp3(filePath)
        "flac" -> extractLyricsFromFlac(filePath)
        "m4a",
        "mp4" -> extractLyricsFromM4a(filePath)
        else -> null
    }
}

fun parsePlainLyrics(lyricsContent: String): List<LyricLine> {
    val list = mutableListOf<LyricLine>()
    val lines = lyricsContent.split("\n")
    var validIndex = 0
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isNotEmpty()) {
            list.add(LyricLine(validIndex * 4000L, trimmed))
            validIndex++
        }
    }
    return list
}

fun loadLyricsForSong(songPath: String): List<LyricLine> {
    val lrcFile = File(songPath.substringBeforeLast('.') + ".lrc")
    if (lrcFile.exists()) {
        try {
            val content = lrcFile.readText(Charsets.UTF_8)
            val list = parseLrc(content)
            if (list.isNotEmpty()) return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val embeddedLyrics = extractLyricsFromAudio(songPath)
    if (!embeddedLyrics.isNullOrBlank()) {
        val list = parseLrc(embeddedLyrics)
        if (list.isNotEmpty()) return list
        return parsePlainLyrics(embeddedLyrics)
    }

    return emptyList()
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricView(
    song: Song,
    playbackProgress: Long,
    appColors: AppColors,
    currentAccent: AccentColor,
    onSeek: (Long) -> Unit,
) {
    var lyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var hasLoaded by remember(song.id) { mutableStateOf(false) }
    var isClickSeeking by remember(song.id) { mutableStateOf(false) }
    var clickedActiveIndex by remember(song.id) { mutableStateOf<Int?>(null) }
    var seekJob by remember(song.id) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(song.id, song.path) {
        withContext(Dispatchers.IO) {
            lyrics = loadLyricsForSong(song.path)
            hasLoaded = true
        }
    }

    if (!hasLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = currentAccent.mainColor)
        }
    } else if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = appColors.textColorSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "暂无歌词", color = appColors.textColorSecondary, fontSize = 15.sp)
            }
        }
    } else {
        val activeIndex =
            remember(lyrics, playbackProgress, clickedActiveIndex) {
                if (clickedActiveIndex != null) {
                    clickedActiveIndex!!
                } else {
                    val idx = lyrics.indexOfLast { it.timeMs <= playbackProgress }
                    if (idx == -1) 0 else idx
                }
            }

        val lyricListState = rememberLazyListState()
        val isUserScrolling by lyricListState.interactionSource.collectIsDraggedAsState()
        var isAutoFollowPaused by remember(song.id) { mutableStateOf(false) }

        LaunchedEffect(isUserScrolling) {
            if (isUserScrolling) {
                isAutoFollowPaused = true
            }
        }

        LaunchedEffect(activeIndex, isClickSeeking) {
            if (
                lyrics.isNotEmpty() &&
                    !lyricListState.isScrollInProgress &&
                    !isClickSeeking &&
                    !isAutoFollowPaused
            ) {
                lyricListState.animateScrollToItem(activeIndex)
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 24.dp)
        ) {
            val halfHeight = maxHeight / 2

            LazyColumn(
                state = lyricListState,
                contentPadding =
                    PaddingValues(top = halfHeight - 24.dp, bottom = halfHeight - 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val textColor by
                        animateColorAsState(
                            targetValue =
                                if (isActive) {
                                    currentAccent.mainColor
                                } else {
                                    appColors.textColorPrimary.copy(alpha = 0.5f)
                                },
                            animationSpec = tween(durationMillis = 350),
                            label = "lyric_color",
                        )
                    val scale by
                        animateFloatAsState(
                            targetValue = if (isActive) 1.15f else 1.0f,
                            animationSpec = tween(durationMillis = 350),
                            label = "lyric_scale",
                        )
                    val scaleAlphaValue by
                        animateFloatAsState(
                            targetValue = if (isActive) 1.0f else 0.4f,
                            animationSpec = tween(durationMillis = 350),
                            label = "lyric_alpha",
                        )

                    Text(
                        text = line.text,
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.fillMaxWidth(0.8f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = scaleAlphaValue
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    clickedActiveIndex = index
                                    isClickSeeking = true
                                    isAutoFollowPaused = false
                                    onSeek(line.timeMs)
                                    seekJob?.cancel()
                                    seekJob =
                                        coroutineScope.launch {
                                            try {
                                                lyricListState.animateScrollToItem(index)
                                                delay(1000)
                                                isClickSeeking = false
                                                clickedActiveIndex = null
                                            } catch (e: kotlinx.coroutines.CancellationException) {}
                                        }
                                }
                                .padding(vertical = 4.dp),
                    )
                }
            }

            val buttonAlpha by
                animateFloatAsState(
                    targetValue = if (isAutoFollowPaused) 1f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "follow_btn_alpha",
                )
            val buttonScale by
                animateFloatAsState(
                    targetValue = if (isAutoFollowPaused) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 300),
                    label = "follow_btn_scale",
                )

            if (buttonAlpha > 0.01f) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 4.dp)
                            .graphicsLayer {
                                alpha = buttonAlpha
                                scaleX = buttonScale
                                scaleY = buttonScale
                            }
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(currentAccent.mainColor.copy(alpha = 0.9f))
                            .clickable {
                                isAutoFollowPaused = false
                                coroutineScope.launch {
                                    if (lyrics.isNotEmpty()) {
                                        lyricListState.animateScrollToItem(activeIndex)
                                    }
                                }
                            },
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "重新跟随",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
