package com.autophoto.ai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Palet mengikuti referensi: netral terang + panel hitam + aksen oranye-merah. */
object AppColors {
    val Surface = Color(0xFFF5F5F5)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF8A8A8E)
    val Dark = Color(0xFF0B0B0C)
    val DarkPanel = Color(0xFF1C1C1E)
    val Accent = Color(0xFFE8472B)
}

@Composable
fun AutoPhotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Ink,
            onPrimary = Color.White,
            background = AppColors.Surface,
            surface = Color.White,
            onSurface = AppColors.Ink
        ),
        content = content
    )
}

/** Atur warna ikon status/navigation bar per layar. */
@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
}
