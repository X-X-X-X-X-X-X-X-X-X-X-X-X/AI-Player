package cn.xuexc.ai_player.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

data class UpdateInfo(val tagName: String, val body: String, val htmlUrl: String)

enum class Screen {
    Library,
    Playlists,
    Artists,
}

enum class AccentColor(
    val id: String,
    val label: String,
    val mainColor: Color,
    val gradientColors: List<Color>,
) {
    TitaniumGray(
        "TitaniumGray",
        "钛金灰",
        Color(0xFF9AA5B1),
        listOf(Color(0xFF486581), Color(0xFFBAC7D5)),
    ),
    DeepBlue("DeepBlue", "极光蓝", Color(0xFF2F80ED), listOf(Color(0xFF2F80ED), Color(0xFF56CCF2))),
    EmeraldGreen(
        "EmeraldGreen",
        "翡翠绿",
        Color(0xFF27AE60),
        listOf(Color(0xFF27AE60), Color(0xFF6FCF97)),
    ),
    FlameRed("FlameRed", "赤焰红", Color(0xFFFF2D55), listOf(Color(0xFFFF2D55), Color(0xFFFF5E7E))),
    SakuraPink("SakuraPink", "樱花粉", Color(0xFFFF8DA1), listOf(Color(0xFFFF8DA1), Color(0xFFFFC5D3))),
}

data class AppColors(
    val mainBackground: Brush,
    val surfaceColor: Color,
    val cardBackground: Color,
    val textColorPrimary: Color,
    val textColorSecondary: Color,
    val navBarBackground: Color,
    val navBarItemActive: Color,
    val navBarItemInactive: Color,
    val textfieldContainer: Color,
    val textfieldBorder: Color,
)

fun getAppColors(accent: AccentColor, isDark: Boolean): AppColors {
    val mainColor =
        if (accent == AccentColor.TitaniumGray) {
            if (isDark) Color(0xFFBAC7D5) else Color(0xFF4A5568)
        } else {
            accent.mainColor
        }
    return if (isDark) {
        AppColors(
            mainBackground = SolidColor(Color(0xFF0C0C0E)),
            surfaceColor = Color(0xFF161619),
            cardBackground = Color(0x0CFFFFFF),
            textColorPrimary = Color(0xFFF5F5F7),
            textColorSecondary = Color(0xFF8E8E93),
            navBarBackground = Color(0xFF121214),
            navBarItemActive = mainColor,
            navBarItemInactive = Color(0x66FFFFFF),
            textfieldContainer = Color(0x0DFFFFFF),
            textfieldBorder = Color.Transparent,
        )
    } else {
        AppColors(
            mainBackground = SolidColor(Color(0xFFF5F5F7)),
            surfaceColor = Color(0xFFFFFFFF),
            cardBackground = Color(0x06000000),
            textColorPrimary = Color(0xFF1C1C1E),
            textColorSecondary = Color(0xFF8E8E93),
            navBarBackground = Color(0xFFF1F1F3),
            navBarItemActive = mainColor,
            navBarItemInactive = Color(0x66000000),
            textfieldContainer = Color(0x06000000),
            textfieldBorder = Color.Transparent,
        )
    }
}
