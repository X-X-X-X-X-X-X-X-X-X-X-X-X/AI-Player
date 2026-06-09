package cn.xuexc.ai_player.playback

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import cn.xuexc.ai_player.MainActivity
import cn.xuexc.ai_player.data.Song
import cn.xuexc.ai_player.data.getCachedCover
import cn.xuexc.ai_player.data.loadCover
import kotlinx.coroutines.*

class PlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateNotificationJob: Job? = null

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "ai_player_playback_channel"

        const val ACTION_START = "cn.xuexc.ai_player.ACTION_START"
        const val ACTION_UPDATE = "cn.xuexc.ai_player.ACTION_UPDATE"
        const val ACTION_PLAY_PAUSE = "cn.xuexc.ai_player.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "cn.xuexc.ai_player.ACTION_NEXT"
        const val ACTION_PREV = "cn.xuexc.ai_player.ACTION_PREV"
        const val ACTION_STOP = "cn.xuexc.ai_player.ACTION_STOP"
        const val ACTION_FAVORITE = "cn.xuexc.ai_player.ACTION_FAVORITE"
        const val ACTION_BLACKLIST = "cn.xuexc.ai_player.ACTION_BLACKLIST"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSessionCompat(this, "AIPlayerMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlaybackManager.resume()
                    updateNotification()
                }

                override fun onPause() {
                    PlaybackManager.pause()
                    updateNotification()
                }

                override fun onSkipToNext() {
                    PlaybackManager.playNext(applicationContext, isUserInitiated = true)
                    updateNotification()
                }

                override fun onSkipToPrevious() {
                    PlaybackManager.playPrevious(applicationContext)
                    updateNotification()
                }

                override fun onSeekTo(pos: Long) {
                    PlaybackManager.seekTo(pos)
                    updateNotification()
                }

                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    when (action) {
                        "ACTION_FAVORITE" -> {
                            PlaybackManager.toggleFavoriteCurrent(applicationContext)
                        }

                        "ACTION_BLACKLIST" -> {
                            PlaybackManager.blacklistCurrent(applicationContext)
                        }
                    }
                }
            })
            isActive = true
        }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY_PAUSE -> {
                val isPlaying = PlaybackManager.isPlaying.value
                if (isPlaying) {
                    PlaybackManager.pause()
                } else {
                    PlaybackManager.resume()
                }
                updateNotification()
            }

            ACTION_NEXT -> {
                PlaybackManager.playNext(applicationContext, isUserInitiated = true)
                updateNotification()
            }

            ACTION_PREV -> {
                PlaybackManager.playPrevious(applicationContext)
                updateNotification()
            }

            ACTION_STOP -> {
                PlaybackManager.stop()
                stopForeground(true)
                stopSelf()
            }

            ACTION_FAVORITE -> {
                PlaybackManager.toggleFavoriteCurrent(applicationContext)
            }

            ACTION_BLACKLIST -> {
                PlaybackManager.blacklistCurrent(applicationContext)
            }

            ACTION_START, ACTION_UPDATE -> {
                updateNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "播放控制通知栏",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供音乐后台播放的状态展示与切歌等交互卡片"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = PlaybackManager.currentSong.value
        val isPlaying = PlaybackManager.isPlaying.value

        if (song == null) {
            stopForeground(true)
            stopSelf()
            return
        }

        // PendingIntents for actions
        val playPauseIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePI = PendingIntent.getService(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPI = PendingIntent.getService(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPI = PendingIntent.getService(
            this,
            3,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favoriteIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_FAVORITE }
        val favoritePI = PendingIntent.getService(
            this,
            5,
            favoriteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val blacklistIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_BLACKLIST }
        val blacklistPI = PendingIntent.getService(
            this,
            6,
            blacklistIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent (Click notification to open app)
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPI = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        updateNotificationJob?.cancel()
        updateNotificationJob = serviceScope.launch {
            // 1. 优先尝试从内存缓存同步获取
            var albumArt = song.getCachedCover()

            // 2. 若没有内存缓存，我们先以“无封面（null）”展示，防止阻塞主线程导致的顿挫
            if (albumArt == null) {
                updateMediaSession(song, isPlaying, null)
                val initNotification = buildNotification(
                    song,
                    isPlaying,
                    null,
                    playPausePI,
                    nextPI,
                    prevPI,
                    favoritePI,
                    blacklistPI,
                    contentPI,
                    playPauseIcon
                )
                startForeground(NOTIFICATION_ID, initNotification)

                // 3. 在后台挂起加载封面
                albumArt = withContext(Dispatchers.IO) {
                    song.loadCover(this@PlaybackService, 200)
                }
            }

            // 4. 获取到封面之后（无论成功与否，或使用上一步已取得的缓存），更新/显示带封面的通知
            updateMediaSession(song, isPlaying, albumArt)
            val notification = buildNotification(
                song,
                isPlaying,
                albumArt,
                playPausePI,
                nextPI,
                prevPI,
                favoritePI,
                blacklistPI,
                contentPI,
                playPauseIcon
            )
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMediaSession(song: Song, isPlaying: Boolean, albumArt: Bitmap?) {
        val favoriteIcon = if (song.isFavorite) {
            cn.xuexc.ai_player.R.drawable.ic_favorite
        } else {
            cn.xuexc.ai_player.R.drawable.ic_favorite_border
        }
        val favoriteLabel = if (song.isFavorite) "取消喜欢" else "设为喜欢"

        val favoriteAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_FAVORITE",
            favoriteLabel,
            favoriteIcon
        ).build()

        val blacklistAction = PlaybackStateCompat.CustomAction.Builder(
            "ACTION_BLACKLIST",
            "加入遗忘沙漏",
            cn.xuexc.ai_player.R.drawable.ic_hourglass
        ).build()

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .addCustomAction(favoriteAction)
            .addCustomAction(blacklistAction)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackManager.playbackProgress.value,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)

        if (albumArt != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
        }
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun buildNotification(
        song: Song,
        isPlaying: Boolean,
        albumArt: Bitmap?,
        playPausePI: PendingIntent,
        nextPI: PendingIntent,
        prevPI: PendingIntent,
        favoritePI: PendingIntent,
        blacklistPI: PendingIntent,
        contentPI: PendingIntent,
        playPauseIcon: Int
    ): Notification {
        val favoriteIcon = if (song.isFavorite) {
            cn.xuexc.ai_player.R.drawable.ic_favorite
        } else {
            cn.xuexc.ai_player.R.drawable.ic_favorite_border
        }
        val blacklistIcon = cn.xuexc.ai_player.R.drawable.ic_hourglass

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setLargeIcon(albumArt)
            .setContentIntent(contentPI)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3) // index matches prev, playPause, next
            )
            .addAction(favoriteIcon, "喜欢", favoritePI)
            .addAction(android.R.drawable.ic_media_previous, "上一首", prevPI)
            .addAction(playPauseIcon, "播放/暂停", playPausePI)
            .addAction(android.R.drawable.ic_media_next, "下一首", nextPI)
            .addAction(blacklistIcon, "加入遗忘沙漏", blacklistPI)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
    }
}
