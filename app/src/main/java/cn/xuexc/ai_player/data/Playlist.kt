package cn.xuexc.ai_player.data

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val firstSongId: Long? = null,
)
