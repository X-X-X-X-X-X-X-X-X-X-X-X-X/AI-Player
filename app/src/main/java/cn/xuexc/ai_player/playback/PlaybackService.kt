package cn.xuexc.ai_player.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import cn.xuexc.ai_player.MainActivity
import cn.xuexc.ai_player.data.Song
import cn.xuexc.ai_player.data.loadCover

class PlaybackService : Service() {

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
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSessionCompat(this, "AIPlayerMediaSession")
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

        // Load album art bitmap as large icon
        val albumArt = song.loadCover(this, 200)

        // PendingIntents for actions
        val playPauseIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePI = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPI = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPI = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Content intent (Click notification to open app)
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPI = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(android.R.drawable.ic_media_previous, "上一首", prevPI)
            .addAction(playPauseIcon, "播放/暂停", playPausePI)
            .addAction(android.R.drawable.ic_media_next, "下一首", nextPI)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPI)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
    }
}
