package cn.xuexc.ai_player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import cn.xuexc.ai_player.ui.SongViewModel
import cn.xuexc.ai_player.ui.components.MainScreen
import cn.xuexc.ai_player.ui.theme.AIPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { SongViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            AIPlayerTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel)
            }
        }
    }
}